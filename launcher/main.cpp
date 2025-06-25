#define ENABLE_VIRTUAL_TERMINAL_PROCESSING 0x0004

#include <iostream>
#include <windows.h>
#include <fstream>
#include <iostream>
#include <vector>
#include <csignal>

using namespace std;


PROCESS_INFORMATION jarProcess;

BOOL WINAPI ConsoleHandler(DWORD signal) {
    if (signal == CTRL_CLOSE_EVENT || signal == CTRL_C_EVENT || signal == CTRL_BREAK_EVENT) {
        if (jarProcess.hProcess) {
            GenerateConsoleCtrlEvent(CTRL_BREAK_EVENT, jarProcess.dwProcessId);
            WaitForSingleObject(jarProcess.hProcess, 3000);
            CloseHandle(jarProcess.hProcess);
            CloseHandle(jarProcess.hThread);
        }
        return TRUE;
    }
    return FALSE;
}

string GetTempJarPath() {
    char tempPath[MAX_PATH];
    GetTempPathA(MAX_PATH, tempPath);

    string tempJar = string(tempPath) + "push_to_talk_logger.jar";
    return tempJar;
}

bool ExtractJarToTemp(const string& outputPath) {
    HRSRC hRes = FindResourceA(NULL, "JARFILE", RT_RCDATA);
    if (!hRes) return false;

    HGLOBAL hData = LoadResource(NULL, hRes);
    if (!hData) return false;

    DWORD size = SizeofResource(NULL, hRes);
    void* pData = LockResource(hData);

    ofstream out(outputPath, ios::binary);
    out.write((const char*)pData, size);
    out.close();

    return true;
}

int main(int argc, char* argv[])
{
    string jarPath = GetTempJarPath();
HRSRC hRes = FindResourceA(NULL, "JARFILE", RT_RCDATA);
    if (!ExtractJarToTemp(jarPath)) {
        cerr << "JAR export error\n";
        return 1;
    }

    HANDLE hOut = GetStdHandle(STD_OUTPUT_HANDLE);
    DWORD dwMode = 0;
    if (GetConsoleMode(hOut, &dwMode)) {
        SetConsoleMode(hOut, dwMode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
    }
    SetConsoleCtrlHandler(ConsoleHandler, TRUE);

    string args;
    for (int i = 1; i < argc; ++i) {
        string arg=argv[i];
        if(arg[0]=='-'){
            arg.erase(0,1);
            arg="-D"+arg;
        }
        if(arg.find("=")==string::npos){
            cerr<<"invalid flag {"<<arg<<"}; missing '='"<<endl;
             if (i + 1 < argc && argv[i + 1][0] != '-') {
                ++i;
            }
           continue;
        }else{
            args += arg;
            args += " ";
        }
    }
    //cout<<"\u001B[96m"<<"101"<<"\u001B[0m";
    string command = "java -jar \"" + jarPath + "\" " + args;

    STARTUPINFOA si = { sizeof(STARTUPINFOA) };
    PROCESS_INFORMATION pi;

    BOOL success = CreateProcessA(
    NULL,
    (LPSTR)command.c_str(),
    NULL,
    NULL,
    TRUE,
    0,
    NULL,
    NULL,
    &si,
    &pi
);

    if (!success) {
        cerr << "launch error: " << GetLastError() << "\n";
        return 1;
    }

    WaitForSingleObject(pi.hProcess, INFINITE);
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    CloseHandle(jarProcess.hProcess);
    CloseHandle(jarProcess.hThread);
    DeleteFileA(jarPath.c_str());

    return 0;
}
