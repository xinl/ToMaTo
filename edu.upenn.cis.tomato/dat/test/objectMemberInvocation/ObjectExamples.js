/*
 * Format of object definition:
 * //type name: ref URL
 * <Object definiton>
 * <invoke function>
*/

//1.simple definition with parameters: http://www.w3schools.com/js/js_objects.asp
function myObject(arg1, arg2) {
//data members:
this.p1 = arg1;
this.p2 = arg2;

//function member
this.showArgs = function(){
	document.write("p1= "+this.p1+" p2= "+this.p2);
	}
this.p3 = "wow";
this.showMsg = function(){ alert("Nice message");}
}

//invoke
function invokeObj1(){
var myNewObj1 = new myObject("1", "2");

alert("what is p3?"+myNewObj1.p3);
// this member function exists


myNewObj1.showArgs();
// this member function doesn't exist


myNewObj1.noexist();
myNewObj1.showMsg();
alert("1");
}

//2.constructor with object parameters
function Person(params)
{
  this.name = params.name ;
  this.role = params.role ;

  if(typeof(this.speak)=='undefined') //guarantees all the subclasses
  {
    Person.prototype.speak = function() {alert("My name is "+this.name)};
  }
}
//invoke
function makeHimSpeak(){
var Robert = new Person({'name':'Bob','role':'mgr'});
Robert.speak();
Robert.sayMgr=function(){alert("manager "+this.role);}
Robert.sayMgr();
alert("2");
}

//3.prototype with future addition: http://www.w3schools.com/jsref/jsref_prototype_math.asp
function employee(nm,jt,bn)
{
this.name=name;
this.jobtitle=jt;
this.born=bn;
}

//invoke
function yabadabado(){
var fred=new employee("Fred Flintstone","Caveman",1970);
// need to be a new function
employee.prototype.salary=function(){alert("msg");}//adding member later
fred.salary();
}


//4. Get and Set method access : ECMAScript standard documentation edition 5.1: http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-262.pdf

function objdef(a1,b1){
	this.a=a1;
	this.b=b1;
	this.showSum = function()
	{
		document.write(this.a+this.b);
		alert("Is this possible?");
	}
}

//invoke:
function invokeObj2(){
	var o = new objdef(1,2);
	o["a"]=3; //setter
	o["showSum"](); //invoke function using getter
	document.write("a= "+o["a"]+" b= "+o["b"]); //getter
	alert("4");
}

//5.JSON access
var JSONObject= {
		"name":"John Johnson",
		"street":"Oslo West 555",
		"age":33,
		"phone":"555 1234567",
		"showDetails":function(){document.write(this.name+","+this.phone)}	};

//invoke
function invokeJSON()
{
	JSONObject.showDetails();
	alert("5");
}

//6. anonymous invoke
function foo() {alert("foo!");}


//function invoke anonymous:
function invokeAnonymous()
{
	var fn=foo;
	// document.write("before foo type of foo is "+typeof foo);
	({}).toString.call(fn()); // == '[object Function]'
	// alert("6");
}

//7:with invoke: http://www.drmaciver.com/2007/03/javascript-with-clause/
//see also slide 40 http://www.slideshare.net/senchainc/javascript-advanced-scoping-other-puzzles
function invokeWith(){
	var obj = {param0:"hi"};
	with(obj)
	{
		param1="hello";
		func1=function(){alert("Cool! using with");}
	}
	
	func1();
	// obj.func1();
	// alert(param1);
}

