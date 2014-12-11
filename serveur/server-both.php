<?php
if ( $_POST['stacktrace'] == "" || $_POST['package_version'] == "" || $_POST['package_name'] == "" ) 
{
	die("This script is used to collect field test crash stacktraces. No personal information is transmitted, collected or stored.<br/>For more information, please contact <a href='mailto:support@nullwire.com'>email@domain.com</a>");
}
		
// Extract data
$version = $_POST['package_version'];
$package = $_POST['package_name'];
$phone_model = $_POST['phone_model'];
$android_version = $_POST['android_version'];
$stacktrace = $_POST['stacktrace'];

// Build stack trace report
$message = 'PHONE MODEL => '. $phone_model ."\nANDROID VERSION => ". $android_version ."\n\n". $stacktrace;

// Write file
$random = rand(1000, 9999);
$handle = fopen($package."-trace-".$version."-".time()."-".$random, "w+");
fwrite($handle, $message);
fclose($handle);

// Send mail
mail("mads.kristiansen@nullwire.com", "IMPORTANT: Exception received (". $version .")", $message, "from:bugs@nullwire.com");
?>
