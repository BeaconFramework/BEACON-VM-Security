# BEACON-FCO-Scanner-Firewall

Three deployable JARs which facilitate taking VM details, passing these details to an openvas deployment and emailing the VM owner a generated security vulnerability report.  Furthermore, a firewall template is generated and deployed to the VM on the FCO platform for additional security measures.

***FCOFirewallExecutable*** is a runnable JAR used only by FCO in order to retrieve a firewall key applied to a VM.  These keys, along with the IP of the server they were applied to are sent to the scanner listener.  The scanner listener then takes this key and IP and uses them to customise the firewall which is applied to a VM upon scan completion, provided the IPs match up.

***FCOExecutable*** is a Runnable JAR which can be called by FCO,Openstack or Open Nebula in order to send VM details to the security scanner listener program.  Upon the creation of a VM with the correct parameters, code is triggered which runs on the FCO platform itself which eventually runs the FCOExecutable. The FCOExecutable then sends the VM information to the server running the VulnerabiliyScanner JAR.

FCOExecutable takes arguments upon launch.  These arguments, minimally should be

***Server UUID,***
*** Server IP,***
*** Email Address***

However, additional arguments can also be given in the form of credentials:

***Username,***
*** Password***

***VulnerabilityScanner*** is a JAR which acts as a listener for the openvas installation.  When the listener detects a request from the FCOExecutable, it gathers the information sent.  This information is then used to set up a security scan of the newly created VM using the openvas server.  Once the scan is complete, the generated report is emailed to the email address associated with the customer account on the FCO.  

After the email containing the report has been sent, a firewall template is built and deployed onto the created VM.

Both Executable JARs use config files to load details.

## FCOExectuable.properties

***scannerIP*** (The IP of the server running the openvas scanner)

***port*** (The port to be used, 8341 default for FCO, 8342 for Openstack and 8343 for OpenNebula)

## scannerConfig.properties

### FCO Credentials

***FCOCloudUsernameCredential,***
*** FCOCloudPasswordCredential,***
*** FCOCloudAdminUUID,***
*** FCOAdminEndpoint,***
*** FCOUserEndpoint***

### Openstack Credentials

***OpenstackCloudUsernameCredential,***
*** OpenstackCloudPasswordCredential,***
*** OpenstackCloudAdminUUID,***
*** OpenstackAdminEndpoint,***
*** OpenstackUserEndpoint***

### OpenNebula Credentials
	
***OpenNebulaCloudUsernameCredential,***
*** OpenNebulaCloudPasswordCredential,***
*** OpenNebulaCloudAdminUUID,***
*** OpenNebulaAdminEndpoint,***
*** OpenNebulaUserEndpoint***



***ActivationScripts*** contains the scripts/triggers used to listen for VM creation and gather details for these new VMs created on their respective platforms.  This includes the VM Creation trigger and Firewall key trigger used on FCO, and the various scripts and hooks used by Openstack and Open Nebula used to listen for newly created VMs.
