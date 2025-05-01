import requests # https://h-mdm.ikgp.de/rest/private/web-ui-files/raw
import sys
import os
import hashlib

if len(sys.argv) != 5:
    print("Usage: python upload_new_apk_to_mdm.py <username> <password> <apk_path> <commit>")
    sys.exit(1)

username = sys.argv[1]
password = sys.argv[2]
apk_path = sys.argv[3]
commit = sys.argv[4]

if not os.path.exists(apk_path):
    print(f"APK file {apk_path} does not exist.")
    sys.exit(1)

client = requests.Session()

resp = client.post("https://h-mdm.ikgp.de/rest/public/auth/login", json={
    "login": username,
    "password": hashlib.md5(password.encode()).hexdigest().upper(),
})
if resp.status_code != 200:
    print(f"Login failed: {resp.status_code} {resp.text}")
    sys.exit(1)

data = resp.json()
if "status" in data and data["status"] == "ERROR":
    print(f"Login failed: {data}")
    sys.exit(1)

# Post the file as form data to https://h-mdm.ikgp.de/rest/private/web-ui-files/raw
with open(apk_path, "rb") as f:
    files = {
        "file": (os.path.basename(apk_path).removesuffix(".apk")+"-" + commit+".apk", f, "application/vnd.android.package-archive"),
    }
    resp = client.post("https://h-mdm.ikgp.de/rest/private/web-ui-files/raw", files=files)
    if resp.status_code != 200:
        print(f"File upload failed: {resp.status_code} {resp.text}")
        sys.exit(1)
    path = resp.json()["data"]["serverPath"]

# POST to https://h-mdm.ikgp.de/rest/private/web-ui-files/move with the path as JSON
resp = client.post("https://h-mdm.ikgp.de/rest/private/web-ui-files/move", json={
    "path": path,
})

# TODO: This gives us a 500, but works anyway
file_path = "https://h-mdm.nirvati.org/files/" + os.path.basename(apk_path).removesuffix(".apk")+"-" + commit+".apk"
print(f"File uploaded to{file_path}")

resp = client.put("https://h-mdm.ikgp.de/rest/private/applications/versions", json={"system":False,"applicationId":18,"arch":None,"url":file_path,"version":commit,"autoUpdate":True})
