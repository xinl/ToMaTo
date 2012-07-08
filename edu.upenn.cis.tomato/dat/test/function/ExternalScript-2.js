var calculator = {multiply: function(a, b){ return a * b; } };

function substraction(f, g)
{ 
  return f-g;
}

function innerAlert2() 
{
 	var z = 2;
 	var o =new Object();
 	o.func = eval;
 	o.func();
 	
 	alert(z);
 	eval();
 	addition(5,9,0);
 	substraction(1,3);
 	addition(10,12);
 	calculator.multiply(2, 3);
}
