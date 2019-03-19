
		      var placeSearch, autocomplete;
		      var componentForm = {
		        street_number: 'short_name',
		        route: 'long_name',
		        locality: 'long_name',
		        administrative_area_level_1: 'short_name',
		        country: 'long_name',
		        postal_code: 'short_name'
		      };

		      function initAutocomplete() {
		        // Create the autocomplete object, restricting the search to geographical
		        // location types.
		        autocomplete = new google.maps.places.Autocomplete(
		            /** @type {!HTMLInputElement} */(document.getElementById('venueAddress')),
		            {});

		        // When the user selects an address from the dropdown, populate the address
		        // fields in the form.
		        autocomplete.addListener('place_changed', fillInAddress);
		      }

		      function fillInAddress() {
		      	var html = '<div style="overflow:hidden;width: 100%;height: 323px;resize:none;max-width:100%;"><div id="google-maps-canvas" style="height:100%; width:100%;max-width:100%;"><iframe style="height:100%;width:100%;border:0;" frameborder="0" src="https://www.google.com/maps/embed/v1/search?key=AIzaSyBIb48XnrVG-JZvYup6AkfvNdCclhEnSwE&q='+$("#venueAddress").val()+'"></iframe></div></div>'
		      	$(".mapArea").html(html)
		      	$(".place-card-large").remove()
		        // Get the place details from the autocomplete object.
		        var place = autocomplete.getPlace();
		        for (var component in componentForm) {
		          //document.getElementById(component).value = '';
		          //document.getElementById(component).disabled = false;
		        }

		        // Get each component of the address from the place details
		        // and fill the corresponding field on the form.
		        for (var i = 0; i < place.address_components.length; i++) {
		          var addressType = place.address_components[i].types[0];
		          if (componentForm[addressType]) {
		            var val = place.address_components[i][componentForm[addressType]];
		            //document.getElementById(addressType).value = val;
		          }
		        }
		      }

		      // Bias the autocomplete object to the user's geographical location,
		      // as supplied by the browser's 'navigator.geolocation' object.
		      function geolocate() {
		        if (navigator.geolocation) {
		          navigator.geolocation.getCurrentPosition(function(position) {
		            var geolocation = {
		              lat: position.coords.latitude,
		              lng: position.coords.longitude
		            };
		            var circle = new google.maps.Circle({
		              center: geolocation,
		              radius: position.coords.accuracy
		            });
		            autocomplete.setBounds(circle.getBounds());
		          });
		        }
		      }
 
    		//Add Venue Form - Cancel Event

		$('#cancelVenue').on('click', function(){
			
			$('#createVenue input').val('');
			$('#createVenue textarea').val('');

		})
