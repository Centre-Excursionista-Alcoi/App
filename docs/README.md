# Working Locally

First, install the required packages (needs Python3 with venv support):

```shell
python3 -mvenv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Then clone the repository and do your changes. To compile:

```shell
make html
```

Then you can see your compiled version in the _build directory.

When you have finished your changes and are satisifed with the preview, create a pull request with your changes.
