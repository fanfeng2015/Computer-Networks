#include <iostream>

using namespace std;

int main() {
    cout << getenv("QUERY_STRING") << " " << getenv("REMOTE_ADDR") << " " << getenv("REMOTE_HOST") << " "
         << getenv("REMOTE_IDENT") << " " << getenv("REMOTE_USER") << " " << getenv("REQUEST_METHOD") << " "
         << getenv("SERVER_NAME") << " " << getenv("SERVER_PORT") << " " << getenv("SERVER_PROTOCOL") << " "
         << getenv("SERVER_SOFTWARE") << endl;
    return 0;
}



