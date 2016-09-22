# BEACON-FCO-Scanner-Firewall

Two deployable JARs which facilitate taking VM details, passing these details to an openvas deployment and emailing the VM owner a generated security vulnerability report.  Furthermore, a firewall template is generated and deployed to the VM on the FCO platform for additional security measures.

FCOExecutable is a Runnable JAR which sits on the FCO management box.  Upon the creation of a VM with the correct parameters, code is triggered which runs on the FCO platform itself which eventually runs the FCOExecutable. The FCOExecutable then sends the VM information to the server running the VulnerabiliyScanner JAR.

VulnerabilityScanner is a JAR which acts as a listener for the openvas installation.  When the listener detects a request from the FCOExecutable, it gathers the information sent.  This information is then used to set up a security scan of the newly created VM using the openvas server.  Once the scan is complete, the generated report is emailed to the email address associated with the customer account on the FCO.  

After the email containing the report has been sent, a firewall template is built and deployed onto the created VM.

Before exporting these projects to JARs, certain parameters will have to set.

### FCOExectuable

***private static final String SCANNER_IP*** (The IP of the server running the openvas scanner)

***private static final int PORT = 8341*** (The port to be used, 8341 default for FCO)

### VulnerabilityScanner

***final String username*** (The username of the email address used to send the report email)

***final String emailpassword*** (The password of the email address used to send the report email)

***String fcousername*** (The username associated with the account used for the API calls)

***String fcopassword*** (The password associated with the account used for the API calls)

***String uuid*** (The UUID associated with the accunt used for the API calls)
