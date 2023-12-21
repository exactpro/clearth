function showhide(sender, id)
{
	e = document.getElementById(id);
	e.style.display = e.style.display == 'block' ? 'none' : 'block';
	sender.style.backgroundImage = e.style.display == 'block' ? 'url(hide.gif)' : 'url(show.gif)';
}

function togglePassed(show)
{
	ShowPassedFields = document.getElementById('ShowPassedFields');
	if (show)
	{
		ShowPassedFields.disabled = true;
		if (ShowPassedFields.checked == false)
		{
			togglePassedFields(true);
			ShowPassedFields.checked = true;
		}
	}
	else
		ShowPassedFields.disabled = false;

	elements = document.getElementsByClassName('action');
	showInverted = document.getElementById('ShowInverted').checked;
	for (var i=0; i<elements.length; i++)
	{
		el = elements[i];
		span = el.getElementsByTagName('span')[0];
		if (span.className.indexOf('passed')>-1)
			if (show)
			{
				if (showInverted)
				{
					if (span.className.indexOf('inverted')>-1)
						el.style.display = '';
					else
						el.style.display = 'none';
				}
				else
					el.style.display = '';
			}
			else
				el.style.display = 'none';
	}
}

function togglePassedFields(show)
{
	elements = document.getElementsByClassName('comparison_line_passed');
	for (var i=0; i<elements.length; i++)
	{
		el = elements[i];
		if (show)
			el.style.display = '';
		else
			el.style.display = 'none';
	}
}

function toggleInverted(show)
{
	elements = document.getElementsByClassName('action');
	showPassed = document.getElementById('ShowPassed').checked;
	for (var i=0; i<elements.length; i++)
	{
		el = elements[i];
		span = el.getElementsByTagName('span')[0];
		if (span.className.indexOf('inverted')==-1)
			if (show)
				el.style.display = 'none';
			else
				if (!showPassed)
				{
					if (span.className.indexOf('passed')>-1)
						el.style.display = 'none';
					else
						el.style.display = '';
				}
				else
					el.style.display = '';
	}
}

function toggleNotExecuted(show)
{
	let steps = document.getElementsByClassName('step');
	for (let i = 0; i < steps.length; i++)
	{
		let step = steps[i];
		let actions = step.getElementsByClassName('action');
		let allNotExecuted = true;
		for (let j = 0; j < actions.length; j++)
		{
			let action = actions[j];
			let actionSpan = action.getElementsByTagName('span')[0];
			if (actionSpan.className.indexOf('not_executed') < 0)
			{
				allNotExecuted = false;
				continue;
			}
			
			if (show)
				action.style.display = '';
			else
				action.style.display = 'none';
		}
		
		if (allNotExecuted && !show)
			step.style.display = 'none';
		else
			step.style.display = '';
	}
}

function toggleExpandAll(show)
{
	var container = document.getElementsByClassName('nodelist')[0];
	var elements = container.getElementsByClassName('step');
	for (var i=0; i<elements.length; i++)
	{
		var cont = elements[i].getElementsByClassName('container')[0];
		if (show)
		{
			if (cont.style.display != 'block')
				elements[i].getElementsByClassName('node')[0].click();
			
			if (!cont.getElementsByClassName('expandStep')[0].checked)
				cont.getElementsByClassName('expandStep')[0].click();
		}
		else
		{
			if (cont.getElementsByClassName('expandStep')[0].checked)
				cont.getElementsByClassName('expandStep')[0].click();
			
			if (cont.style.display != 'none')
				elements[i].getElementsByClassName('node')[0].click();
		}
	}
}

function toggleExpandStep(step, show)
{
	var stepContainer = document.getElementById(step);
	var elements = stepContainer.getElementsByClassName('action');
	for (var i=0; i<elements.length; i++)
	{
		var cont = elements[i].getElementsByClassName('container')[0];
		if (show)
		{
			if (cont.style.display != 'block')
				elements[i].getElementsByClassName('node')[0].click();
		}
		else
			if (cont.style.display != 'none')
				elements[i].getElementsByClassName('node')[0].click();

		var comps = cont.getElementsByClassName('comps')[0].getElementsByClassName('status');
		for (var j=0; j<comps.length; j++)
		{
			cont = comps[j].getElementsByClassName('container')[0];
			if (show)
			{
				if (cont.style.display != 'block')
					showhide(comps[j].getElementsByClassName('switch')[0], cont.id);
			}
			else
				if (cont.style.display != 'none')
					showhide(comps[j].getElementsByClassName('switch')[0], cont.id);
		}
	}
}

function showAllConstants()
{
	var els = document.getElementsByClassName('hiddenConstant');
	var i;
	for (i = 0; i < els.length; i++)
	{
		els[i].style.display = 'block';
	}
	document.getElementById('showAllConstButton').style.display = 'none';
}