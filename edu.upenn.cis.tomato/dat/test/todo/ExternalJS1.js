//phishing

function hello()
{
var url="http://www.google.com";
window.location.assign(url);
};

function myObject()
{
	this.name="abc";
	this.getName=function(){
		alert(this.name);		
	};
};

function getObjectName()
{
	var o = new myObject();
	o.getName(); // this doesn't work
	hello(); // this works
}

getObjectName();