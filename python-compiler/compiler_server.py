#!/usr/bin/env python3
"""
Multi-language code compiler server
Similar to the Kotlin version but supports multiple languages
"""

import os
import json
import tempfile
import subprocess
import shutil
from dataclasses import dataclass, asdict
from typing import List, Optional, Dict, Callable, Tuple
from flask import Flask, request, jsonify, make_response
from flask_cors import CORS

# Fix PATH for Kotlin compiler - ensure proper path concatenation
kotlin_bin_path = r'C:\kotlinc\bin'
current_path = os.environ.get('PATH', '')

# Only add if not already in PATH and if the directory exists
if kotlin_bin_path not in current_path and os.path.exists(kotlin_bin_path):
    os.environ['PATH'] = kotlin_bin_path + os.pathsep + current_path
    print(f"Added {kotlin_bin_path} to PATH")
elif os.path.exists(kotlin_bin_path):
    print(f"Kotlin path already in PATH: {kotlin_bin_path}")
else:
    print(f"Kotlin path does not exist: {kotlin_bin_path}")

app = Flask(__name__)
CORS(app)

@dataclass
class CompileRequest:
    code: str
    language: str
    fileName: Optional[str] = None

@dataclass
class CompileResponse:
    success: bool
    output: str
    errors: List[str]

class LanguageConfig:
    def __init__(self, extension: str, compile_cmd: Optional[Callable] = None, 
                 run_cmd: Callable = None, default_filename: str = None):
        self.extension = extension
        self.compile_cmd = compile_cmd
        self.run_cmd = run_cmd
        self.default_filename = default_filename

def check_command_exists(cmd: str) -> bool:
    """Check if a command exists in the system PATH"""
    try:
        # For Windows, try both the command and .bat version
        commands_to_try = [cmd]
        if os.name == 'nt' and cmd == 'kotlinc':
            commands_to_try = ['kotlinc.bat', 'kotlinc']
        
        for command in commands_to_try:
            try:
                # Different commands have different version flags
                version_flags = {
                    'kotlinc': ['-version'],
                    'kotlinc.bat': ['-version'],
                    'kotlin': ['-version'],
                    'javac': ['-version'],
                    'java': ['-version'],
                    'python': ['--version'],
                    'node': ['--version'],
                    'gcc': ['--version'],
                    'g++': ['--version'],
                    'go': ['version']
                }
                
                flag = version_flags.get(command, ['--version'])
                
                print(f"Trying command: {command} with flags {flag}")
                
                # For Windows, use shell=True to handle .bat files properly
                result = subprocess.run(
                    [command] + flag, 
                    capture_output=True, 
                    timeout=15, 
                    text=True,
                    shell=(os.name == 'nt')  # Use shell on Windows
                )
                
                print(f"Result for {command}: returncode={result.returncode}")
                if result.stdout:
                    print(f"  stdout: {result.stdout.strip()[:100]}")
                if result.stderr:
                    print(f"  stderr: {result.stderr.strip()[:100]}")
                
                # For kotlinc, check if it ran without errors or if output contains version info
                if command in ['kotlinc', 'kotlinc.bat']:
                    success = result.returncode == 0 or 'kotlin' in result.stderr.lower() or 'kotlin' in result.stdout.lower()
                    if success:
                        print(f"✓ {command} found and working!")
                        return True
                elif result.returncode == 0:
                    return True
                    
            except FileNotFoundError:
                print(f"Command {command} not found in PATH")
                continue
            except subprocess.TimeoutExpired:
                print(f"Command {command} timed out")
                continue
            except Exception as e:
                print(f"Exception with {command}: {e}")
                continue
        
        return False
        
    except Exception as e:
        print(f"General exception checking {cmd}: {e}")
        return False

def kotlin_compile_cmd(file_path: str, temp_dir: str) -> List[str]:
    """Generate Kotlin compilation command"""
    jar_path = os.path.join(temp_dir, 'program.jar')
    # Use kotlinc.bat explicitly on Windows
    kotlinc_cmd = 'kotlinc.bat' if os.name == 'nt' else 'kotlinc'
    return [kotlinc_cmd, file_path, '-include-runtime', '-d', jar_path]

def kotlin_run_cmd(file_path: str, temp_dir: str) -> List[str]:
    """Generate Kotlin run command"""
    jar_path = os.path.join(temp_dir, 'program.jar')
    return ['java', '-jar', jar_path]

def kotlin_interpret_cmd(file_path: str, temp_dir: str) -> List[str]:
    """Alternative: Try to run Kotlin as script (if kotlin command exists)"""
    kotlin_cmd = 'kotlin.bat' if os.name == 'nt' else 'kotlin'
    return [kotlin_cmd, '-script', file_path]

# Language configurations
LANGUAGE_CONFIGS = {
    'python': LanguageConfig(
        extension='.py',
        compile_cmd=None,  # Python is interpreted
        run_cmd=lambda file_path, temp_dir: ['python', file_path],
        default_filename='main.py'
    ),
    'java': LanguageConfig(
        extension='.java',
        compile_cmd=lambda file_path, temp_dir: ['javac', file_path],
        run_cmd=lambda file_path, temp_dir: [
            'java', '-cp', temp_dir, 
            os.path.splitext(os.path.basename(file_path))[0]
        ],
        default_filename='Main.java'
    ),
    'kotlin': LanguageConfig(
        extension='.kt',
        compile_cmd=kotlin_compile_cmd,
        run_cmd=kotlin_run_cmd,
        default_filename='Main.kt'
    ),
    'c': LanguageConfig(
        extension='.c',
        compile_cmd=lambda file_path, temp_dir: [
            'gcc', file_path, '-o', os.path.join(temp_dir, 'program.exe')
        ],
        run_cmd=lambda file_path, temp_dir: [os.path.join(temp_dir, 'program.exe')],
        default_filename='main.c'
    ),
    'cpp': LanguageConfig(
        extension='.cpp',
        compile_cmd=lambda file_path, temp_dir: [
            'g++', file_path, '-o', os.path.join(temp_dir, 'program.exe')
        ],
        run_cmd=lambda file_path, temp_dir: [os.path.join(temp_dir, 'program.exe')],
        default_filename='main.cpp'
    ),
    'javascript': LanguageConfig(
        extension='.js',
        compile_cmd=None,  # JavaScript is interpreted
        run_cmd=lambda file_path, temp_dir: ['node', file_path],
        default_filename='main.js'
    ),
    'go': LanguageConfig(
        extension='.go',
        compile_cmd=None,  # Go compiles and runs in one step
        run_cmd=lambda file_path, temp_dir: ['go', 'run', file_path],
        default_filename='main.go'
    )
}

def execute_command(cmd: List[str], cwd: str, timeout: int = 30) -> Tuple[str, str, int]:
    """Execute a command and return stdout, stderr, and return code"""
    try:
        print(f"Executing command: {' '.join(cmd)} in {cwd}")
        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            cwd=cwd,
            timeout=timeout,
            env=os.environ.copy(),  # Ensure we use the updated environment
            shell=(os.name == 'nt')  # Use shell on Windows for .bat files
        )
        print(f"Command completed with return code: {result.returncode}")
        return result.stdout, result.stderr, result.returncode
    except subprocess.TimeoutExpired:
        return "", "Execution timed out", 1
    except FileNotFoundError as e:
        return "", f"Command not found: {cmd[0]} - {e}", 1
    except Exception as e:
        return "", f"Execution error: {str(e)}", 1

def compile_and_run_code(compile_request: CompileRequest) -> CompileResponse:
    """Compile and run code based on the language"""
    
    # Validate language
    if compile_request.language not in LANGUAGE_CONFIGS:
        return CompileResponse(
            success=False,
            output="",
            errors=[f"Unsupported language: {compile_request.language}"]
        )
    
    config = LANGUAGE_CONFIGS[compile_request.language]
    
    # Special handling for Kotlin if kotlinc is not available
    if compile_request.language == 'kotlin':
        kotlinc_available = check_command_exists('kotlinc')
        kotlin_available = check_command_exists('kotlin')
        
        print(f"Kotlin check: kotlinc={kotlinc_available}, kotlin={kotlin_available}")
        
        if not kotlinc_available:
            if kotlin_available:
                # Try to use kotlin script runner instead
                config.compile_cmd = None
                config.run_cmd = kotlin_interpret_cmd
                print("Using Kotlin script runner instead of kotlinc")
            else:
                return CompileResponse(
                    success=False,
                    output="",
                    errors=[
                        "Kotlin compiler not found. Troubleshooting info:",
                        f"Kotlin bin path exists: {os.path.exists(kotlin_bin_path)}",
                        f"kotlinc.bat exists: {os.path.exists(os.path.join(kotlin_bin_path, 'kotlinc.bat'))}",
                        f"Current PATH contains kotlinc: {kotlin_bin_path in os.environ.get('PATH', '')}",
                        "Try running kotlinc.bat -version manually in terminal to test"
                    ]
                )
    
    # Determine filename
    if compile_request.fileName:
        filename = compile_request.fileName
    else:
        filename = config.default_filename
    
    # Create temporary directory
    with tempfile.TemporaryDirectory() as temp_dir:
        file_path = os.path.join(temp_dir, filename)
        
        # Write code to file
        try:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(compile_request.code)
        except Exception as e:
            return CompileResponse(
                success=False,
                output="",
                errors=[f"Failed to write code to file: {str(e)}"]
            )
        
        errors = []
        compile_output = ""
        
        # Compilation step (if needed)
        if config.compile_cmd:
            print(f"Compiling {compile_request.language} code...")
            compile_cmd = config.compile_cmd(file_path, temp_dir)
            stdout, stderr, returncode = execute_command(compile_cmd, temp_dir, timeout=60)
            
            compile_output = stdout + stderr
            
            if returncode != 0:
                return CompileResponse(
                    success=False,
                    output=compile_output,
                    errors=[f"Compilation failed with exit code: {returncode}"]
                )
        
        # Execution step
        print(f"Running {compile_request.language} code...")
        run_cmd = config.run_cmd(file_path, temp_dir)
        stdout, stderr, returncode = execute_command(run_cmd, temp_dir)
        
        # Format output
        output_parts = []
        if compile_output:
            output_parts.append("Compilation successful")
        
        if stdout:
            output_parts.append("--- Execution Output ---")
            output_parts.append(stdout.strip())
        
        if stderr:
            output_parts.append("--- Execution Error ---")
            output_parts.append(stderr.strip())
            
        if not stdout and not stderr:
            output_parts.append("--- No Output ---")
        
        if returncode != 0 and stderr:
            errors.append(f"Execution failed with exit code: {returncode}")
        
        final_output = "\n".join(output_parts) if output_parts else compile_output
        
        return CompileResponse(
            success=returncode == 0 and not errors,
            output=final_output,
            errors=errors
        )

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    response = make_response("OK")
    response.headers['Access-Control-Allow-Origin'] = '*'
    return response

@app.route('/compile', methods=['POST', 'OPTIONS'])
def compile_endpoint():
    """Main compilation endpoint"""
    
    # Handle CORS preflight
    if request.method == 'OPTIONS':
        response = make_response()
        response.headers['Access-Control-Allow-Origin'] = '*'
        response.headers['Access-Control-Allow-Methods'] = 'POST, OPTIONS'
        response.headers['Access-Control-Allow-Headers'] = 'Content-Type'
        return response, 204
    
    try:
        # Parse request
        if not request.is_json:
            return jsonify(asdict(CompileResponse(
                success=False,
                output="",
                errors=["Content-Type must be application/json"]
            ))), 400
        
        data = request.get_json()
        print(f"Received request: {json.dumps(data, indent=2)}")
        
        if not data:
            return jsonify(asdict(CompileResponse(
                success=False,
                output="",
                errors=["Empty request body"]
            ))), 400
        
        # Validate required fields
        if 'code' not in data or 'language' not in data:
            return jsonify(asdict(CompileResponse(
                success=False,
                output="",
                errors=["Missing required fields: code and language"]
            ))), 400
        
        # Create compile request
        compile_request = CompileRequest(
            code=data['code'],
            language=data['language'].lower(),
            fileName=data.get('fileName')
        )
        
        # Compile and run
        result = compile_and_run_code(compile_request)
        
        print(f"Sending response: {json.dumps(asdict(result), indent=2)}")
        
        response = jsonify(asdict(result))
        response.headers['Access-Control-Allow-Origin'] = '*'
        response.headers['Content-Type'] = 'application/json'
        
        return response
        
    except Exception as e:
        print(f"Server error: {str(e)}")
        error_response = CompileResponse(
            success=False,
            output="",
            errors=[f"Server error: {str(e)}"]
        )
        
        response = jsonify(asdict(error_response))
        response.headers['Access-Control-Allow-Origin'] = '*'
        response.headers['Content-Type'] = 'application/json'
        
        return response, 500

@app.route('/languages', methods=['GET'])
def get_supported_languages():
    """Get list of supported languages"""
    languages = []
    for lang in LANGUAGE_CONFIGS.keys():
        if lang == 'kotlin':
            if check_command_exists('kotlinc') or check_command_exists('kotlin'):
                languages.append(lang)
        else:
            languages.append(lang)
    
    response = jsonify({
        'languages': languages,
        'count': len(languages)
    })
    response.headers['Access-Control-Allow-Origin'] = '*'
    return response

if __name__ == '__main__':
    print("Compiler server starting on port 5000...")
    print(f"Operating System: {os.name}")
    print(f"Kotlin bin path: {kotlin_bin_path}")
    print(f"Kotlin bin path exists: {os.path.exists(kotlin_bin_path)}")
    print(f"kotlinc.bat exists: {os.path.exists(os.path.join(kotlin_bin_path, 'kotlinc.bat'))}")
    print(f"kotlinc exists: {os.path.exists(os.path.join(kotlin_bin_path, 'kotlinc'))}")
    
    # Check available languages
    available_languages = []
    for lang in LANGUAGE_CONFIGS.keys():
        if lang == 'kotlin':
            kotlinc_exists = check_command_exists('kotlinc')
            kotlin_exists = check_command_exists('kotlin')
            print(f"Debug: kotlinc exists = {kotlinc_exists}, kotlin exists = {kotlin_exists}")
            
            if kotlinc_exists:
                available_languages.append(f"{lang} (kotlinc)")
            elif kotlin_exists:
                available_languages.append(f"{lang} (script)")
            else:
                print(f"⚠️  {lang}: compiler not found")
        else:
            available_languages.append(lang)
    
    print("Available languages:", available_languages)
    print("Test with:")
    print('curl -X POST -H "Content-Type: application/json" -d \'{"code":"fun main() { println(\\"Hello from Kotlin!\\") }","language":"kotlin"}\' http://localhost:5000/compile')
    
    app.run(host='127.0.0.1', port=5000, debug=True)
