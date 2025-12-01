import argparse
import os
import plistlib
import re

def replace_plist_property(plist_path: str, property_name: str, new_value: str):
    """
    Replaces the specified property in the given plist file with the new value.

    Args:
        plist_path (str): The path to the plist file.
        property_name (str): The name of the property to replace.
        new_value (str): The new value for the property.
    """

    try:
        # Load the plist file
        with open(plist_path, 'rb') as f:
            plist_data = plistlib.load(f)

        # Find and replace the property
        plist_data[property_name] = new_value

        # Write the modified plist back to the file
        with open(plist_path, 'wb') as f:
            plistlib.dump(plist_data, f)

        print(f"Successfully replaced property '{property_name}' in {plist_path}")
    except Exception as e:
        print(f"Error replacing property: {e}")

def replace_marketing_version(file_path: str, version_name: str):
    """Replaces the MARKETING_VERSION property in an iOS project with the given version name.

    Args:
        file_path: The path to the project file.
        version_name: The new version name to set.
    """

    lines = []
    with open(file_path, "r") as f:
        lines = f.readlines()
        for i in range(len(lines)):
            line = lines[i]
            if "MARKETING_VERSION" in line:
                lines[i] = re.sub('MARKETING_VERSION ?= ?\\d+\\.\\d+\\.\\d+', f"MARKETING_VERSION = {version_name}", line)
    os.remove(file_path)
    with open(file_path, "w") as f:
        f.writelines(lines)
