import os
import shutil
from typing import Callable

def enumerateFolder(folderName: str, action: Callable[[str, str], None]):
    for entry in os.listdir(folderName):
        print(entry)
        if os.path.isfile(folderName + "/" + entry):
            action(folderName, entry)
        else:
            enumerateFolder(folderName + "/" + entry, action)

wpiFiles: list[str] = []
wpiDir = "/home/Avery/Documents/allwpilib/wpiutil/src/main"

def addWPIFile(folderName, fileName):
    if fileName[-3:] == 'cpp':
        wpiFiles.append(folderName + "/" + fileName) 

def moveWPIFile(folderName, fileName):
    fileName = fileName[0:-1] + "cpp"
    print(fileName)
    candidates: list[str] = []
    for file in wpiFiles:
        if file.endswith("/"+fileName):
            candidates.append(file)
    wholeTarget = folderName + "/" + fileName
    if os.path.exists(wholeTarget):
        return

    elif len(candidates) == 1:
        shutil.copy(candidates[0], wholeTarget)

    else: print(candidates)



enumerateFolder(wpiDir, addWPIFile)

print(wpiFiles)

enumerateFolder("/home/Avery/Documents/psikit/native/src/main/cpp", moveWPIFile)
