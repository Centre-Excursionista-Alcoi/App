import argparse
import os
import os.path as path
import re

root = path.join(path.dirname(__file__), '..')
versionFile = path.join(root, "version.properties")

if not path.exists(versionFile):
    print(f"Version file ({versionFile}) doesn't exist.")
    exit(1)

parser = argparse.ArgumentParser(description="Update Version Name")
parser.add_argument('--name', action="store", dest='name', default=None)
args = parser.parse_args()
version_name: str|None = args.name

if version_name is None:
    print("Version name not specified.")
    print("Usage: python3 updateVersionName.py --name=1.0.0")
    exit(1)

# Verify that the version name is valid
version_pieces = version_name.split(".")
if len(version_pieces) > 3 or len(version_pieces) < 1:
    print("Version name should be composed of between 1 and 3 integers separated by '.'")
    print("Examples: 1.0.0 1.0")
    exit(1)

for piece in version_pieces:
    if not piece.isdigit():
        print("Version name should be composed of between 1 and 3 integers separated by '.'")
        print("Examples: 1.0.0 1.0")
        exit(1)

print(f"Updating Version to: {version_name}")

new_lines = None

with open(versionFile) as f:
    lines = f.readlines()
    for i in range(len(lines)):
        line = lines[i]
        if line.startswith("VERSION_NAME="):
            lines[i] = f"VERSION_NAME={version_name}\n"
    new_lines = lines

os.remove(versionFile)
with open(versionFile, "w") as f:
    f.writelines(new_lines)
