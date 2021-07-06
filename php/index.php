<?php
	$ip = file_get_contents("../mospeed/.ip");
	if (!$ip) {
		echo("Configuration error!");
		die();
	}
	header("Location: http://".$ip.":8192/WebPetscii");
	die();
?>