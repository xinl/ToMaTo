// ----- Basic test ----- //

function systemAlias()
{
  var loc = window;
  loc.location = "http://www.google.com";
}

function globalAlias()
{  
  var lVariable = gVariable;
}

function myCars()
{
	this.driver = "human";
}

function userAlias()
{  
  var myCars = new myCars();
  var myTrucks = myCars;
}

function mySuperArrayTest()
{
	var firstArray = new Array();
	var secondArray = new Array();
}

//----- Advanced test ----- //

function userAliasConditional(args)
{  
  var myCars = new Array();
  var myTrucks;
  
  if(args > 0)
  {
    myTrucks = myCars;
  }
  else
  {
    myTrucks = new Array();
  }
}

function systemLiteral()
{
  var loc = "http://www.google.com";
  window.location = loc;
}


