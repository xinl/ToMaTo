function userAlias2()
{ 
  var a = "address 1";
  var b = a;
  b = "address 2";
  var c = a + b;
}

function userAlias3()
{ 
  var a;
  var b;
  a = window.location;
  b = a;
  a = "address 2";
}

function userAlias4()
{  
  var myCars = new Array();
  var myTrucks;
  
  myTrucks = myCars;
  myTrucks[0] = "Saab";
  
}

function systemAlias1()
{
  // Alias of system data structure
  var loc = { x : 1 };
  loc.x = window.location;
  loc.x = "http://example.com/";
  loc = window.location;
  loc = "http://example.com/"
}

function systemAlias2()
{
  // Alias of system data structure
  var loc = new Array();
  loc[0]="http://example.com";
  window.location = loc;
  var b= window.location;
  b="http://example.com";
}

function userAlias1(args)
{  
  alert("In the 1st function");
  var myCars=new Array();
  var myTruck;
  
  if(args>0)
  {
    myTruck = myCars;
  }
  else
  {
    myTruck = new Array();
  }
  
  myTruck[0]="Saab";
  alert(myCars[0]);
}

function userAlias2()
{ 
  var a = "address 1";
  var b = a;
  alert(b);
  b = "address 2";
  alert(a);
}

function userAlias3()
{ 
  var a;
  var b;
  a = window.location;
  b = a;
  alert(b);
  a = "address 2";
  alert(a);
  alert(b);
}

function systemAlias1()
{
  // Alias of system data structure
  var loc = { x : 1 };
  alert(loc.x);
  loc.x = window.location;
  alert(loc.x);
  loc.x = "http://example.com/";
  
  loc = window.location;
  alert(loc);
  loc = "http://example.com/"
}



function systemAlias3()
{
  // Alias of system data structure
  var loc = new Array();
  loc[0]="http://example.com";
  window.location = loc;
  loc[0]="http://www.google.com";
  window.location = loc;
}