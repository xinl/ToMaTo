<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>Rad Upload Plus</title>
</head>
<script type="text/javascript" src="script.js"></script>

	<body>

<?	
		$useApplet=0;
		$user_agent = $_SERVER['HTTP_USER_AGENT'];
	   
		if(stristr($user_agent,"konqueror") || stristr($user_agent,"macintosh") || stristr($user_agent,"opera"))
		{ 		
			$useApplet=1;
			echo '<applet name="jsapplet"
					archive="jsapplet.jar"
					code="com.raditha.articles.JSHelloWorld"
					width="300" MAYSCRIPT name="jsap" id="jsap"
					height="300">';
			
		}
		else
		{			   
			if(strstr($user_agent,"MSIE")) { 
				echo '<object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"
					width= "290" height= "290" style="border-width:0;"  id="rup" name="rup"
					codebase="http://java.sun.com/products/plugin/autodl/jinstall-1_4_1-windows-i586.cab#version=1,4,1" name="jsap" id="jsap">';
					
			} else {
				echo '<object type="application/x-java-applet;version=1.4.1"
					width= "300" height= "300"  name="jsap" id="jsap">';
			} 
			echo '	<param name="archive" value="jsapplet.jar">
				<param name="code" value="com.raditha.articles.JSHelloWorld">
				<param name="mayscript" value="yes">
				<param name="scriptable" value="true">
				<param name="name" value="jsapplet">';
				
		}
		if($useApplet==1)
		{
			echo '</applet>';
		}
		else
		{
			echo '</object>';
		}
?>
 </body>
</html>

