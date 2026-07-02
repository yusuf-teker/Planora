import re

with open('iosApp/iosApp.xcodeproj/project.pbxproj', 'r') as f:
    content = f.read()

# Remove from PBXBuildFile
content = re.sub(r'.*FirebaseAnalytics in Frameworks.*?\n', '', content)
content = re.sub(r'.*FirebaseCrashlytics in Frameworks.*?\n', '', content)
content = re.sub(r'.*FirebaseMessaging in Frameworks.*?\n', '', content)

# Remove from packageProductDependencies
content = re.sub(r'.*FirebaseAnalytics.*?,.*?\n', '', content)
content = re.sub(r'.*FirebaseCrashlytics.*?,.*?\n', '', content)
content = re.sub(r'.*FirebaseMessaging.*?,.*?\n', '', content)

# Remove XCSwiftPackageProductDependency sections
content = re.sub(r'/\* Begin XCSwiftPackageProductDependency section \*/.*?/\* End XCSwiftPackageProductDependency section \*/\n', '', content, flags=re.DOTALL)
content = re.sub(r'/\* Begin XCRemoteSwiftPackageReference section \*/.*?/\* End XCRemoteSwiftPackageReference section \*/\n', '', content, flags=re.DOTALL)


with open('iosApp/iosApp.xcodeproj/project.pbxproj', 'w') as f:
    f.write(content)

print("Firebase dependencies removed from project.pbxproj")
