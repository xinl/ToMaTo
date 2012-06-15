function myObject(arg1, arg2) {

    //data members:
    this.p1 = arg1;
    this.p2 = arg2;

    //function member
    var fun1 = function(){ document.write("p1= "+this.p1+" p2= "+this.p2);};
    this.showArgs = fun1;
    this.showMsg = function(){ alert("message");}
    
    var unrelatedVar = 42;
}

//invoke
function invokeObj1(){
    var myNewObj1 = new myObject("1", "2");
    var foo = myNewObj1.showArgs;
    foo();
    //myNewObj1.showArgs();
    myNewObj1.showMsg();
}

function testAlias() {
    invokeObj1();
}