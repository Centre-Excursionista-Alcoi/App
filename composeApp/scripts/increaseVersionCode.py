import os
import os.path as path

codeFile = path.join(path.dirname(__file__), '..', 'code.txt')

if not path.exists(codeFile):
    print(f"Code file ({codeFile}) doesn't exist.")
    exit(1)

code = 1

# Read the file line by line, and replace the version code with the next one
with open(codeFile) as f:
    # Get the contents of the file
    code_str = f.read()
    # Convert to it, and sum 1
    code = int(code_str) + 1

os.remove(codeFile)
with open(codeFile, "w") as f:
    f.write(str(code))
