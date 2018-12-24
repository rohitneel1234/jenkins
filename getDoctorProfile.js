$(document).ready(function() {
	$('form').submit(function(event) {

	 var formData = {
            "EmailId":$('input[name=usernm]').val()
        };
		var json = JSON.stringify(formData);
		//console.log(json);
    $.ajax({
        type:"POST",
        dataType: "json",
        contentType:"application/json",
        data:json,
        url:"https://jx6u7d3at5.execute-api.us-west-2.amazonaws.com/development/getdoctorprofile"
    }).done(function(data) {
		console.log(data.FirstName);
         $.post("file:///home/rohit/MDX%20(3)/home.html", function (data) {
                    		//alert(data); // <-- add this code
                                    $("name").html(data.FirstName);
                             });
      //$('span').append('Dr.'+data.FirstName+&nbsp;+data.LastName);
    });
    event.preventDefault();
	});
});
