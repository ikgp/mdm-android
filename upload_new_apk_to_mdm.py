import requests  # https://h-mdm.ikgp.de/rest/private/web-ui-files/raw
import sys
import os
import hashlib

if len(sys.argv) != 5:
    print(
        "Usage: python upload_new_apk_to_mdm.py <username> <password> <apk_path> <commit>"
    )
    sys.exit(1)

username = sys.argv[1]
password = sys.argv[2]
apk_path = sys.argv[3]
commit = sys.argv[4]

if not os.path.exists(apk_path):
    print(f"APK file {apk_path} does not exist.")
    sys.exit(1)

client = requests.Session()

resp = client.post(
    "https://h-mdm.ikgp.de/rest/public/auth/login",
    json={
        "login": username,
        "password": hashlib.md5(password.encode()).hexdigest().upper(),
    },
)
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
        "file": (
            os.path.basename(apk_path).removesuffix(".apk") + "-" + commit + ".apk",
            f,
            "application/vnd.android.package-archive",
        ),
    }
    resp = client.post("https://h-mdm.ikgp.de/rest/private/web-ui-files", files=files)
    if resp.status_code != 200:
        print(f"File upload failed: {resp.status_code} {resp.text}")
        sys.exit(1)
    data = resp.json()["data"]
    path = data["serverPath"]
    realVersion = data["fileDetails"]["version"]
    versionCode = data["fileDetails"]["versionCode"]


resp = client.put(
    "https://h-mdm.ikgp.de/rest/private/applications/versions",
    json={
        "system": False,
        "applicationId": 18,
        "arch": None,
        "pkg": "com.hmdm.launcher",
        "name": "MDM Agent",
        "version": realVersion,
        "versionCode": versionCode,
        "filePath": path,
    },
)

id = resp.json()["data"]["id"]
file_path = (
    "https://h-mdm.ikgp.de/files/"
    + os.path.basename(apk_path).removesuffix(".apk")
    + "-"
    + commit
    + ".apk"
)

resp = client.put(
    "https://h-mdm.ikgp.de/rest/private/applications/versions",
    json={
        "id": id,
        "applicationId": 18,
        "version": commit,
        "versionCode": versionCode,
        "url": file_path,
        "split": False,
        "urlArmeabi": None,
        "urlArm64": None,
        "deletionProhibited": False,
        "commonApplication": False,
        "system": False,
        "type": "app",
        "apkHash": None,
        "arch": None,
        "filePath": None,
    },
)

print(resp.text)

payload = {
    "applicationVersionId": id,
    "configurations": [
        {
            "id": None,
            "customerId": 1,
            "configurationId": 4,
            "configurationName": "Entwicklermodus",
            "applicationId": 18,
            "applicationName": "MDM Agent",
            "applicationVersionId": id,
            "versionText": commit,
            "showIcon": True,
            "screenOrder": None,
            "keyCode": None,
            "bottom": False,
            "longTap": False,
            "action": "1",
            "remove": False,
            "notify": True,
            "common": False,
            "selected": True,
        },
        {
            "id": None,
            "customerId": 1,
            "configurationId": 1,
            "configurationName": "Standard-Tafel",
            "applicationId": 18,
            "applicationName": "MDM Agent",
            "applicationVersionId": id,
            "versionText": commit,
            "showIcon": False,
            "screenOrder": None,
            "keyCode": None,
            "bottom": False,
            "longTap": False,
            "action": "1",
            "remove": False,
            "notify": True,
            "common": False,
            "selected": True,
        },
    ],
}

print(payload)

client.post(
    "https://h-mdm.ikgp.de/rest/private/applications/version/configurations",
    json=payload,
)

print(resp.text)
