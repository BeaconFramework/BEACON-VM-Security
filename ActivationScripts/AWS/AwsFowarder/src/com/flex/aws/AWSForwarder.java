package com.flex.aws;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

public class AWSForwarder{ 
    public static ArrayList<String> awsinstances = new ArrayList<String>();
	public static void main(String[] args) {
    //    new AWSTest();
        // Reperat every 60 seconds call to AWS API
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(awsRunnable, 0, 60, TimeUnit.SECONDS);
    }
	
	
	static Runnable awsRunnable = new Runnable() {
	public void run() {
		//Pull values from aws.properties file
		 String awsSecretkey ="";
		 String awsAccesskey="";
		 String awsEndpoint =""; 
		 String userEmail ="";
		 String vmUsername="";
		 String vmPassword = "";
		 Properties mainProperties = new Properties();

		 try {

		    FileInputStream file;

		    String path = "./aws.properties";

		    file = new FileInputStream(path);

		    mainProperties.load(file);
		   
			file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		 
		 // Pull config details from properties file.
		    awsSecretkey = mainProperties.getProperty("awsSecretkey");
		    awsAccesskey = mainProperties.getProperty("awsAccesskey");
		    awsEndpoint = mainProperties.getProperty("awsEndpoint");
		    userEmail = mainProperties.getProperty("userEmail");
		    vmUsername = mainProperties.getProperty("vmUsername");
		    vmPassword = mainProperties.getProperty("password");
		    //Create AWS API call
			AmazonEC2 ec2;
			AWSCredentials credentials = new BasicAWSCredentials(awsAccesskey, awsSecretkey);
	        ec2 = new AmazonEC2Client(credentials);
	        ec2.setEndpoint(awsEndpoint);
	     //For each VM pull the status and forward details to jar file   
	     DescribeInstancesResult insResult = ec2.describeInstances();
	     
	    for (Reservation reservation : insResult.getReservations())
	    {
	      for(Instance instance : reservation.getInstances())
	      {
	    	  if (instance.getState().getName().equals("running") && !awsinstances.contains(instance.getInstanceId()))
	    	  {
	  		  		String vmUUID = instance.getInstanceId();

	    		 awsinstances.add(vmUUID);
  		  		System.out.println("Instance tags:" + instance.getTags().get(0).getKey() + instance.getKeyName());
  		  		
  		  		String firstkey = instance.getTags().get(0).getKey();
  		  		String defaultsshkey = instance.getKeyName();
  		  		String vmIP = instance.getPublicIpAddress();
		    	  // call forwarder & pass VM UUID, IP
		  		ProcessBuilder pb = new ProcessBuilder("java", "-jar", "your.jar", vmUUID, vmIP, userEmail,vmUsername, vmPassword);
		  		try {
					Process p = pb.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	 }
	    	  		else 
	    	  		{
	    		  		System.out.println("Instance exists:" + instance.getInstanceId());
	    		  		System.out.println("STATE exists:" + instance.getState().getName());
	    	  		}
	      		}
	    	 }
	      }
	};
}
	
