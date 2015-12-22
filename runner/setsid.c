// setsid -- spawns given command in its own session id and process group
#include <unistd.h>
#include <string.h>
#include <stdio.h>
int main(int argc, char *argv[]) {
    // use setsid(2) to become a new session and process group leader
    if (setsid() < 0) {
        perror("setsid");
        // continue executing given command as it can handle the error better
    }
    // forward all command-line arguments intact
    char *cmd = argv[1], *args[argc];
    args[0] = cmd;
    for (int i = 2; i < argc; ++i) {
        args[i - 1] = argv[i];
    }
    args[argc - 1] = NULL;
    // use execvp(2) to execute the given command
    execvp(cmd, args);
    // code beyond this line should not be reachable unless execvp fails
    perror("execvp");
    return 255;
}
