This simple routine is used to identify when a new VM is instantiated on a cloud.
This is made comparing VMs uuid list returned by the Openstack agent with a list of VMs scanned stored inside an inner repository. 
If a new VM is recognized the system retrieve the information of that VM and launch an isntance of the JAVA activation script with the parameters required for that VM.

The scripts are lauched each 3 minutes by a cron job.
