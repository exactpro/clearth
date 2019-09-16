/*
Usage:

<div id="tabs">
	<div class="tab active">Tab 1</div>
	<div class="tab">Tab 2</div>
	<div class="tab">Tab 3</div>
	<div class="tabContent">1</div>
	<div class="tabContent">2</div>
	<div class="tabContent">3</div>
</div>
*/

var tab;
var tabContent;

window.onload = function() {
	tabContent = document.getElementsByClassName('tabContent');
	tab = document.getElementsByClassName('tab');
	hideTabsContent(1);
}

document.getElementById('tabs').onclick = function(event) {
	var target = event.target;
	if (target.className == 'tab') {
		for (var i = 0; i < tab.length; i++) {
			if (target == tab[i]) {
				showTabsContent(i);
				break;
			}
		}
	} else return;
}

function hideTabsContent(a) {
	for (var i = a; i < tabContent.length; i++) {
		tabContent[i].classList.remove('show');
		tabContent[i].classList.add("hide");
		tab[i].classList.remove('active');
	}
}

function showTabsContent(b) {
	if (tabContent[b].classList.contains('hide')) {
		hideTabsContent(0);
		tab[b].classList.add('active');
		tabContent[b].classList.remove('hide');
		tabContent[b].classList.add('show');
	}
}