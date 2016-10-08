//******************************************************************************
//     This is JavaScript support for SQRL's periodic page refresh probe.
//------------------------------------------------------------------------------
// This script begins running when it is invoked, typically at the top of the
// page through a script declaration in the page's head. There is no need for,
// nor benefit in, deliberately placing this at the bottom of the page, though
// that can be done if it's preferable for any reason.
//
// The script's sole purpose is to periodically check to see whether it should
// cause a page refresh, usually to display the result of a SQRL authentication.
// It simply queries SQRL's webserver for the value of a 'sync.txt' page. When
// the returned data differs from the data it first obtained, it triggers a
// refresh of the page.
//------------------------------------------------------------------------------
// LICENSE AND COPYRIGHT:  LIKE ALL OF SQRL, THIS CODE IS HEREBY RELEASED INTO
// THE PUBLIC DOMAIN. Gibson Research Corporation releases and disclaims ALL
// RIGHTS AND TITLE IN THIS CODE OR ANY DERIVATIVES. It may be used and/or
// modified and used by anyone for any purpose.
//******************************************************************************

if (window.XMLHttpRequest)
	var syncQuery = new XMLHttpRequest();
else
	var syncQuery = new ActiveXObject('MSXML2.XMLHTTP.3.0');	// for ancient IE.

function checkForChange() {					// before probing for any page change,
	if (document.hidden) {					// let's see whether the page is visible?
		setTimeout(checkForChange, 5000);	// the user is not viewing the page,
		return;								// so check again in 5 seconds
	}
	syncQuery.open( 'GET', 'sqrlauto' );	// the page is visible, so let's check for any update
	syncQuery.onreadystatechange = function() {
		if ( syncQuery.readyState == 4 ) {
			if ( syncQuery.status == 200 ) {
				newSync = syncQuery.responseText;
				if (typeof lastSync !== 'undefined' && lastSync != newSync)
					document.location.href = document.location.pathname.substring(location.pathname.lastIndexOf("/") + 1);
				else
					lastSync = newSync;
			}
			setTimeout(checkForChange, 500); // after every query, successful or not, retrigger after 500msc.
		}	
	}
	syncQuery.send(); // initiate the query to the 'sync.txt' object.
}

checkForChange();	// this launches the first instance of "checkForChange"
					// which then self-retriggers periodically to recheck.
