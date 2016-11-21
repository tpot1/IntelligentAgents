cd adx-server   

start cmd /k call runServer.bat

cd ../adx-agent

call compile.bat

start cmd /k call runAgentTestAdNet.bat

