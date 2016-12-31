<%@page import="com.universeprojects.miniup.server.ServletMessager"%>
<%@page import="com.universeprojects.miniup.server.services.CombatService"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="com.universeprojects.miniup.server.ODPDBAccess.ScriptType"%>
<%@page import="com.universeprojects.miniup.server.services.MainPageUpdateService"%>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c'%>
<%@page import="com.universeprojects.miniup.server.services.CaptchaService"%>
<%@page import="com.universeprojects.miniup.server.HtmlComponents"%>
<%@page import="java.util.Date"%>
<%@page import="com.universeprojects.cacheddatastore.CachedEntity"%>
<%@page import="com.universeprojects.miniup.server.longoperations.LongOperation"%>
<%@page import="com.universeprojects.miniup.server.TradeObject"%>
<%@page import="com.universeprojects.cacheddatastore.CachedDatastoreService"%>
<%@page import="com.google.appengine.api.datastore.DatastoreService"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.universeprojects.miniup.server.GameUtils"%>
<%@page import="com.universeprojects.miniup.server.GameFunctions"%>
<%@page import="com.google.appengine.api.datastore.Key"%>
<%@page import="java.util.List"%>
<%@page import="com.universeprojects.miniup.server.PrefixCodes"%>
<%@page import="com.universeprojects.miniup.server.WebUtils"%>
<%@page import="com.universeprojects.miniup.server.SecurityException"%>
<%@page import="com.universeprojects.miniup.server.CommonEntities"%>
<%@page import="com.universeprojects.miniup.server.ErrorMessage"%>
<%@page import="com.universeprojects.miniup.server.JspSnippets"%>
<%@page import="com.google.appengine.api.datastore.Entity"%>
<%@page import="com.universeprojects.miniup.server.ODPDBAccess"%>
<%@page import="com.universeprojects.miniup.server.Authenticator"%>
<%
	response.setHeader("Access-Control-Allow-Origin", "*");		// This is absolutely necessary for phonegap to work

	if (request.getServerName().equals("www.playinitium.appspot.com"))
	{ 
		response.setStatus(301);
		response.setHeader("Location", "http://www.playinitium.com");
		return;
	}


	Authenticator auth = Authenticator.getInstance(request);
	GameFunctions db = auth.getDB(request);
	CachedDatastoreService ds = db.getDB();
	try
	{
		auth.doSecurityChecks(request);
	}
	catch(SecurityException e)
	{
		JspSnippets.handleSecurityException(e, request, response);
		return;
	}

	request.setAttribute("isThrowawayInSession", auth.isThrowawayCharacterInSession());
	if (auth.isThrowawayCharacterInSession())
	{
		request.setAttribute("throwawayName", auth.getThroawayCharacter(ds).getProperty("name"));
	}
	

	
	// Check if the user has a verified account. If not, send them back to the quickstart page with a message...
	/*
	if (db.getCurrentUser()!=null && db.getCurrentUser().getProperty("verified")==null)
	{
		String pleaseVerify = "Please verify your email address account before playing!<br>" + 
				"You should have received a verification email already. If you haven't, you can <a onclick='resendVerificationEmail()'>click here to resend the verification email</a>.<br>" + 
				"If you wish to change your email address, you can <a onclick='changeEmailAddress(&quot;"+db.getCurrentUser().getProperty("email")+"&quot;)'>do that here</a>.";
		WebUtils.forceRedirectClientTo("quickstart.jsp", request, response, pleaseVerify);
		return;
	}
	*/
	

	boolean botCheck = new CaptchaService(db).isBotCheckTime();
	request.setAttribute("botCheck", botCheck);
	
	
	
	if (GameUtils.isPlayerIncapacitated(db.getCurrentCharacter()))
	{
		WebUtils.forceRedirectClientTo("killed.jsp", request, response);
		return;
	}

	CombatService combatService = new CombatService(db);
	
	String characterMode = (String)db.getCurrentCharacter().getProperty("mode");
	if (combatService.isInCombat(db.getCurrentCharacter()))
	{
		// This has to be "ask for" because forced redirections will include the isRedirection and thus "Looter Protection" will not work
		WebUtils.forceRedirectClientTo("combat.jsp", request, response);
		return;
	}
	if (characterMode!=null && characterMode.equals(GameFunctions.CHARACTER_MODE_TRADING))
	{
		request.setAttribute("isTrading", true);
	}
	
	// We need some user entity attributes
	Boolean isPremium = false;
	if (db.getCurrentUser()!=null)
		isPremium = (Boolean)db.getCurrentUser().getProperty("premium");
	if (isPremium==null) isPremium = false;
	request.setAttribute("isPremium", isPremium);
	request.setAttribute("characterDogecoinsFormatted", GameUtils.formatNumber(db.getCurrentCharacter().getProperty("dogecoins")));
	
	// Get the referral url
	String referralUrl = GameUtils.determineReferralUrl(db.getCurrentUser());
	request.setAttribute("referralUrl", referralUrl);
	
	// Determine if a long operation is in progress and get the recall javascript if it is
	String longOperationRecallJs = LongOperation.getLongOperationRecall(db, db.getCurrentCharacter().getKey());
	request.setAttribute("longOperationRecallJs", longOperationRecallJs);
	
	// Get all the characters that this user has (if he is a premium member)
	List<CachedEntity> characterList = null;
	if (isPremium && db.getCurrentUser()!=null)
	{
		characterList = db.getFilteredList("Character", "userKey", db.getCurrentUser().getKey());
		request.setAttribute("characterList", characterList);
	}
	
	request.setAttribute("characterName", db.getCurrentCharacter().getProperty("name"));
	request.setAttribute("characterId", db.getCurrentCharacter().getKey().getId());
	request.setAttribute("chatIdToken", ServletMessager.generateIdToken(db.getCurrentCharacter().getKey()));
	
	if (db.getCurrentUser()!=null)
		request.setAttribute("characterToTransfer", db.getCurrentUser().getProperty("transferCharacterName"));
	
	// Get the prefix code so we can add some special text to the description in special cases
	String prefix = "";
	Integer prefixCode = WebUtils.getIntParam(request, "pre");
	if (prefixCode!=null)
	{
		prefix = PrefixCodes.getTextForPrefixCode(prefixCode);
		if (prefix!=null && "null".equals(prefix)==false)
	request.setAttribute("prefix", prefix);
	}
	
	
	CachedEntity location = db.getEntity((Key)db.getCurrentCharacter().getProperty("locationKey"));
	if (location==null)
	{
		location = db.getEntity(db.getDefaultLocationKey());
		db.getCurrentCharacter().setProperty("locationKey", location.getKey());
	}
	request.setAttribute("locationName", location.getProperty("name"));
	
	String biome = (String)location.getProperty("biomeType");
	if (biome==null) biome = "Temperate";
	request.setAttribute("biome", biome);
	String locationAudioDescriptor = (String)location.getProperty("audioDescriptor");
	if (locationAudioDescriptor==null) locationAudioDescriptor = "";
	request.setAttribute("locationAudioDescriptor", locationAudioDescriptor);

	
	
	String locationAudioDescriptorPreset = (String)location.getProperty("audioDescriptorPreset");
	if (locationAudioDescriptorPreset==null) locationAudioDescriptorPreset = "";
	request.setAttribute("locationAudioDescriptorPreset", locationAudioDescriptorPreset);
	
	
	
	
	
	boolean combatSite = false;
	if (((String)location.getProperty("name")).startsWith("Combat site: "))
		combatSite = true;
	request.setAttribute("combatSite", combatSite);

	
	
	Double monsterCount = db.getMonsterCountForLocation(ds, location);
	Double maxMonsterCount = (Double)location.getProperty("maxMonsterCount");
	request.setAttribute("maxMonsterCount", maxMonsterCount);
	if (monsterCount!=null && maxMonsterCount!=null)
		request.setAttribute("monsterCountRatio", monsterCount/maxMonsterCount);
	else
		request.setAttribute("monsterCountRatio", 0d);
	
	
	request.setAttribute("supportsCamps", location.getProperty("supportsCamps"));
	boolean isOutside = false;
	if ("TRUE".equals(location.getProperty("isOutside")))
		isOutside = true;
	
	request.setAttribute("isOutside", isOutside);
	
	// Party related stuff
	/*
	List<CachedEntity> party = null;
	if (db.getCurrentCharacter().getProperty("partyCode")!=null)
	{
		party = db.getParty(ds, db.getCurrentCharacter());
		
		if (party!=null)
		{
	request.setAttribute("isPartied", true);
	request.setAttribute("party", party);
	request.setAttribute("partyCount", party.size());
	request.setAttribute("isPartyLeader", "TRUE".equals(db.getCurrentCharacter().getProperty("partyLeader")));
		}
	}
	
	// Determine the party leader (or yourself if not in a party)
	CachedEntity partyLeader = null;
	if (party!=null)
	{
		for(CachedEntity e:party)
	if ("TRUE".equals(e.getProperty("partyLeader")))
		partyLeader = e;
	}
	else
		partyLeader = db.getCurrentCharacter();
	
	if (partyLeader!=null && partyLeader.equals("")==false)
	{
		if (partyLeader.getKey().getId() == db.getCurrentCharacter().getKey().getId())
	request.setAttribute("isPartyLeader", true);
		else
	request.setAttribute("isPartyLeader", false);
		
		if ("TRUE".equals(partyLeader.getProperty("partyJoinsAllowed")))
	request.setAttribute("partyJoinsAllowed", true);
		else
	request.setAttribute("partyJoinsAllowed", false);
	}
*/		
	
	
	/////////////////////////////////////////////////////
	// This part pertains to blockade options...

	List<CachedEntity> charactersHere = null;
	CachedEntity leader = null;	
	CachedEntity defenceStructure = db.getEntity((Key)location.getProperty("defenceStructure"));
	if (defenceStructure!=null) 
	{
		// This is a defence structure location and has some additional UI elements as such
		request.setAttribute("isDefenceStructure", true);

		String defenceMode = (String)defenceStructure.getProperty("blockadeRule");
		if (defenceMode==null) defenceMode="BlockAllParent";
		request.setAttribute("defenceMode", defenceMode);

		// The class that should be used for each defence mode option
		if (defenceMode.equals("BlockAllParent"))
	request.setAttribute("defenceModeBlockAllParent", "selected-item");
		if (defenceMode.equals("BlockAllSelf"))
	request.setAttribute("defenceModeBlockAllSelf", "selected-item");
		if (defenceMode.equals("None"))
	request.setAttribute("defenceModeNone", "selected-item");
		
		// Find out who the leader is and generate the html used to render his name on the page
		leader = db.getEntity((Key)defenceStructure.getProperty("leaderKey"));
		request.setAttribute("leader", leader);
		if (leader!=null && db.getCurrentCharacter().getKey().getId() == leader.getKey().getId())
	request.setAttribute("isLeader", true);
	
		
		
		String status = (String)db.getCurrentCharacter().getProperty("status");
		if (status==null || status.equals("") || status.equals("Normal"))		// Normalize the status for easy testing
	status = null;
		request.setAttribute("characterStatus", status);
		
		if ("Defending1".equals(status))
		{
	request.setAttribute("statusDescription", "You are part of the first line of defence if someone were to attack this location.");
	request.setAttribute("nextStatusDescription", "Click here to change to the second line of defence");
		}
		else if ("Defending2".equals(status))
		{
	request.setAttribute("statusDescription", "You are part of the second line of defence if someone were to attack this location.");
	request.setAttribute("nextStatusDescription", "Click here to change to the third line of defence");
		}
		else if ("Defending3".equals(status))
		{
	request.setAttribute("statusDescription", "You are part of the third line of defence if someone were to attack this location.");
	request.setAttribute("nextStatusDescription", "Click here to stop defending");
		}
		else
		{
	request.setAttribute("statusDescription", "You are not currently defending this location.");
	request.setAttribute("nextStatusDescription", "Click here to defend this structure as the first line of defence");
		}
	
		
		request.setAttribute("defenceStructureHitpoints", GameUtils.formatNumber(defenceStructure.getProperty("hitpoints")));
		request.setAttribute("defenceStructureMaxHitpoints", GameUtils.formatNumber(defenceStructure.getProperty("maxHitpoints")));
		
		
		// Now lets get some info on everyone who is here
		charactersHere = db.getFilteredList("Character", "locationKey", location.getKey());
		
		int defending1 = 0;
		int defending2 = 0;
		int defending3 = 0;
		int defendingEngaged1 = 0;
		int defendingEngaged2 = 0;
		int defendingEngaged3 = 0;
		
		int notDefending = 0;
		for(CachedEntity chr:charactersHere)
		{
	String chrStatus = (String)chr.getProperty("status");
	if ("Defending1".equals(chrStatus) && (Double)chr.getProperty("hitpoints")>0)
	{
		defending1++;
		
		if ("COMBAT".equals(chr.getProperty("mode")))
	defendingEngaged1++;
	}
	else if ("Defending2".equals(chrStatus) && (Double)chr.getProperty("hitpoints")>0)
	{
		defending2++;

		if ("COMBAT".equals(chr.getProperty("mode")))
	defendingEngaged2++;
	}
	else if ("Defending3".equals(chrStatus) && (Double)chr.getProperty("hitpoints")>0)
	{
		defending3++;

		if ("COMBAT".equals(chr.getProperty("mode")))
	defendingEngaged3++;
	}
	else if ((Double)chr.getProperty("hitpoints")>0)
		notDefending++;
		}
		
		request.setAttribute("defender1Count", defending1);
		request.setAttribute("defender2Count", defending2);
		request.setAttribute("defender3Count", defending3);
		request.setAttribute("defenderEngaged1Count", defendingEngaged1);
		request.setAttribute("defenderEngaged2Count", defendingEngaged2);
		request.setAttribute("defenderEngaged3Count", defendingEngaged3);
		request.setAttribute("notDefendingCount", notDefending);
	}
	else
	{
		request.setAttribute("isDefenceStructure", false);
	}
	
	
	//////////////////////////////
	// Collection site stuff
	
	if ("CollectionSite".equals(location.getProperty("type")))
	{
		request.setAttribute("isCollectionSite", true);
	}
	
	
	// Instance respawn javascript
	if ("TRUE".equals(location.getProperty("instanceModeEnabled")) && location.getProperty("instanceRespawnDate")!=null)
	{
		Date respawnDate = (Date)location.getProperty("instanceRespawnDate");
		request.setAttribute("instanceRespawnMs", respawnDate.getTime());
	}
	else
		request.setAttribute("instanceRespawnMs", "null");
	
	long currentTimeMs = System.currentTimeMillis();
	String clientDescription = db.getClientDescriptionAndClear(null, db.getCurrentCharacter().getKey());	// This should be near the end of the jsp's java head to reduce the chance of being redirected away from the page before the message gets displayed
	if (clientDescription==null || "null".equals(clientDescription)) clientDescription = "";
	request.setAttribute("clientDescription", clientDescription);
	if (request.getAttribute("midMessage")==null || "null".equals(request.getAttribute("midMessage")))
		request.setAttribute("midMessage", "");
	
	
	
	
	// Orders...
	if (db.getCurrentUser()!=null)
		request.setAttribute("usedCustomOrders", db.getCurrentUser().getProperty("usedCustomOrders"));
	
	
	
	MainPageUpdateService updateService = new MainPageUpdateService(db, db.getCurrentUser(), db.getCurrentCharacter(), location, null);
	
	request.setAttribute("bannerTextOverlay", updateService.updateInBannerOverlayLinks());
	request.setAttribute("mainButtonList", updateService.updateButtonList(combatService));
	request.setAttribute("bannerJs", updateService.updateLocationJs());	
	request.setAttribute("activePlayers", updateService.updateActivePlayerCount());
	request.setAttribute("buttonBar", updateService.updateButtonBar());
	request.setAttribute("locationDescription", updateService.updateLocationDescription());
	request.setAttribute("territoryViewHtml", updateService.updateTerritoryView());
	request.setAttribute("partyPanel", updateService.updatePartyView());
	request.setAttribute("locationScripts", updateService.updateLocationDirectScripts());
	request.setAttribute("inBannerCharacterWidget", updateService.updateInBannerCharacterWidget());
	
	
	if (db.getCurrentCharacter().isUnsaved())
		db.getDB().put(db.getCurrentCharacter());
%>



<!doctype html>
<html>
<head>
	<jsp:include page="common-head2.jsp"/><jsp:include page="odp/common-head.jsp"/>
	<title>Main - Initium</title>



<script type='text/javascript'>
	$(document).ready(function (){
		<c:if test="${combatSite==true}">
		loadInlineItemsAndCharacters();
		</c:if>
		
		<c:if test="${isCollectionSite==true}">
		loadInlineCollectables();
		</c:if>
		
		// Request permission to use desktop notifications
		notifyHandler.requestPermission();		
	});
</script>

<script type='text/javascript' src='odp/javascript/banner-weather.js?v=5'></script>

<script id='ajaxJs' type='text/javascript'>
${bannerJs}
</script>

<script type='text/javascript'>
	if (isAnimationsEnabled())
	{
		$.preload("https://initium-resources.appspot.com/images/anim/walking.gif", 
				"https://initium-resources.appspot.com/images/anim/props/tree1.gif",
				"https://initium-resources.appspot.com/images/anim/props/tree2.gif",
				"https://initium-resources.appspot.com/images/anim/props/tree3.gif",
				"https://initium-resources.appspot.com/images/anim/props/tree4.gif",
				"https://initium-resources.appspot.com/images/anim/props/tree5.gif",
				"https://initium-resources.appspot.com/images/anim/props/tree6.gif",
				"https://initium-resources.appspot.com/images/anim/props/shrub1.gif",
				"https://initium-resources.appspot.com/images/anim/props/shrub2.gif",
				"https://initium-resources.appspot.com/images/anim/props/shrub3.gif",
				"https://initium-resources.appspot.com/images/anim/props/baretree1.gif",
				"https://initium-resources.appspot.com/images/anim/props/baretree2.gif",
				"https://initium-resources.appspot.com/images/anim/props/baretree3.gif",
				"https://initium-resources.appspot.com/images/anim/props/baretree4.gif",
				"https://initium-resources.appspot.com/images/anim/props/baretree5.gif",
				"https://initium-resources.appspot.com/images/anim/props/baretree6.gif",
				"https://initium-resources.appspot.com/images/anim/props/baretree7.gif",
				"https://initium-resources.appspot.com/images/anim/props/grass1.gif",
				"https://initium-resources.appspot.com/images/anim/props/grass2.gif",
				"https://initium-resources.appspot.com/images/anim/props/grass3.gif",
				"https://initium-resources.appspot.com/images/anim/props/grass4.gif",
				"https://initium-resources.appspot.com/images/anim/props/grass5.gif",
				"https://initium-resources.appspot.com/images/anim/props/grass6.gif"
				);
	}
</script>

<script type='text/javascript' src='odp/javascript/messager-impl.js?v=16'></script>

<script type='text/javascript' src='odp/javascript/soundeffects.js?v=1'></script>
<script type='text/javascript'>
	// THIS SECTION IS NEEDED FOR THE SOUND EFFECTS
	$(document).ready(function(){
		setAudioDescriptor("${locationAudioDescriptor}", "${locationAudioDescriptorPreset}", <c:out value="${isOutside}"/>);
	});
</script>

<script type='text/javascript'>
${longOperationRecallJs}
</script>

<script type='text/javascript'>
	/*Antibot stuff*/
	<c:if test="${botCheck==true}">
		function onCaptchaLoaded(){
			antiBotQuestionPopup();
		}
	</c:if>
</script>

<script type='text/javascript'>
	/*Other javascript variables*/
	window.isPremium = ${isPremium};
</script>

</head>

<!--
		HEY!!
		
Did you know you can help code Initium?
Check out our github and get yourself setup,
then talk to the lead dev so you can get yourself
on our slack channel!

http://github.com/Emperorlou/Initium-ODP 

                                           -->
<body>
	<%@ include file="loggedin-header.jspf" %>  
	<div class='main-page'>
		<img class='main-page-banner-image' src="https://initium-resources.appspot.com/images/banner-backing.jpg" border=0/>
		<div style="position:absolute; top:27px;z-index:1000100;">
		<img class='main-page-banner-image' src="https://initium-resources.appspot.com/images/banner-backing.jpg" border=0/>
		
		<div class='main-banner' >
			<img class='main-page-banner-image' src="https://initium-resources.appspot.com/images/banner---placeholder.gif" border=0/>
			<c:if test="${isOutside=='TRUE' }">
				<div class='banner-shadowbox'>
			</c:if>
			<c:if test="${isOutside!='TRUE' }">
				<div class='banner-shadowbox' style="background: url('https://initium-resources.appspot.com/images/banner---placeholder.gif') no-repeat center center">
			</c:if>
				
			
				<div style="overflow:hidden;position:absolute;width:100%;height:100%;">
					<div id='banner-base' class='banner-daynight'></div>
					<div id='banner-text-overlay'>${bannerTextOverlay}</div>
				
					<div id='inBannerCharacterWidget' class='characterWidgetContainer'>
						${inBannerCharacterWidget}
					</div>				
				</div>
				</div>
			</div>
		</div>
		
		
 		<div class='chat_box above-page-popup'>
			<div class='chat_tab_container'>
				<span class='chat_tab_button_container'><a id='chat_box_minimize_button' onclick='toggleMinimizeBox(event, ".chat_box");' class='chat_tab_toggle_minimize'>V</a></span><span class='chat_tab_button_container'><a id='GameMessages_tab' class="chat_tab" onclick='changeChatTab("GameMessages")'><span class='chat-button-indicator' id='GameMessages-chat-indicator'></span>!</a></span><span class='chat_tab_button_container'><a id='GlobalChat_tab' class='chat_tab chat_tab_selected' onclick='changeChatTab("GlobalChat")'><span class='chat-button-indicator' id='GlobalChat-chat-indicator'></span>Global</a></span><span class='chat_tab_button_container'><a id='LocationChat_tab' class='chat_tab' onclick='changeChatTab("LocationChat")'><span class='chat-button-indicator' id='LocationChat-chat-indicator'></span>Location</a></span><span class='chat_tab_button_container'><a id='GroupChat_tab' class="chat_tab" onclick='changeChatTab("GroupChat")'><span class='chat-button-indicator' id='GroupChat-chat-indicator'></span>Group</a></span><span class='chat_tab_button_container'><a id='PartyChat_tab' class="chat_tab" onclick='changeChatTab("PartyChat")'><span class='chat-button-indicator' id='PartyChat-chat-indicator'></span>Party</a></span><span class='chat_tab_button_container'><a id='PrivateChat_tab' class="chat_tab" onclick='changeChatTab("PrivateChat")'><span class='chat-button-indicator' id='PrivateChat-chat-indicator'></span>Private</a></span><a onclick='helpPopup();' class='chat_tab_help_button'>?</a>
			</div>
			<div id="chat_tab">
				<div id="chat_form_wrapper">
					<form id="chat_form">
						<span class='fullscreenChatButton' onclick='toggleFullscreenChat()'>[&nbsp;]</span><input type='hidden' id='chatroomId' value='L<%=location.getKey().getId()%>' />
						<div class='chat_form_input'>
							<input id="chat_input" type="text" autocomplete="off" placeholder='Chat with anyone else in this location' maxlength='2000'/>
						</div>
						<div class='chat_form_submit'>
							<input id="chat_submit" type="submit" value='Submit'/>
						</div>
					</form>
				</div>
				<div class='chat_messages' id="chat_messages_GameMessages">This is not yet used. It will be where you can see all of the game messages, in particular - combat messages.</div>
				<div class='chat_messages' id="chat_messages_GlobalChat"></div>
				<div class='chat_messages' id="chat_messages_LocationChat"></div>
				<div class='chat_messages' id="chat_messages_GroupChat"></div>
				<div class='chat_messages' id="chat_messages_PartyChat"></div>
				<div class='chat_messages' id="chat_messages_PrivateChat"></div>
			</div>
			<div class='chat_tab_footer'>
				<a class='clue' rel='/odp/ajax_ignore.jsp' style='float:left'>Ignore Players</a>
				<span id='ping' title='Your connection speed with the server in milliseconds'>??</span>
				&#8226; 
				<c:if test="${usedCustomOrders}">
					<a onclick='customizeItemOrderPage()'>View Custom Orders</a> 
					&#8226; 
				</c:if>
				<a onclick='viewReferrals()'>View Active Referral Urls</a> 
				&#8226; 
				Active players: <span id='activePlayerCount'>${activePlayers}</span>
			</div>
			<script type="text/javascript">updateMinimizeBox("#chat_box_minimize_button", ".chat_box")</script>
		</div>

		<div id='page-popup-root'></div> 

		<div id='territoryView'>
		${territoryViewHtml}
		</div>
		
		<c:if test="${isDefenceStructure}">
			<div class='hiddenTooltip' id='defenders'>
				<h5>Active Defenders</h5>
				Defenders are player characters (or NPCs in the case of an event) that will defend against attacking players. Players can only defend
				while in a defence structure like the one you're looking at now. There are 4 stances: 1st Line, 2nd Line, 3rd Line, and Not Defending.
				Defenders that are in the 1st line will always be the first to enter combat. If no other defenders are available when an attacker
				approaches, then the 2nd Line stanced players will engage...etc. Players that are 'Not Defending' will never engage in PvP, however
				if the building becomes overrun, they can be kicked out by the attacking players. 
			</div>
			<div class='hiddenTooltip' id='defenderCount'>This is the number of defenders that are set to defend in this particular stance.</div>
			<div class='hiddenTooltip' id='engagedDefenders'>This is the number of defenders that are currently engaged in combat.</div>
			<div class='hiddenTooltip' id='joinedDefenderGroup'>This means that you are part of this group of defenders (1st line, 2nd line, 3rd line, or not defending). To choose a different stance, click on the one of the Join links.</div>
			<div class='boldbox' id='defenceStructureBox'>
				<div class='hiddenTooltip' id='defencestructures'>
					<h5>Defence Structures</h5>
					<p>
						Defence structures are used in PvP and territory control. These structures have the capability
						of blocking other players from entering into the location that the structure stands. The leader
						of the structure has some control over this and is able to set the defence mode to:
						<ul>
						<li>Blockade all players from passing through the location the structure was built in</li>
						<li>Defend only the structure itself from intruders</li>
						<li>Disable all defences and open the structure up to the public</li>
						</ul>
						When the defence structure is in defence mode, there needs to be people in the 1st, 2nd, or 3rd 
						defence lines. Otherwise, the structure is publicly accessible and anyone could take it over without
						opposition. To become a leader of a defence structure, you need to be the first person to enter
						the defensive lines. This can be done after all of the defenders are killed, or if the structure is
						unoccupied or simply not defended.
					</p>
					<p>
						Defenders of a structure are always passively defending, even when the player is offline. After
						each combat with an attacking player, the defender will always heal fully; an attacking player must
						kill the defender without running to heal. Loot dropped by killed defenders are dropped within the 
						structure whereas loot dropped by attackers are dropped in the location the attacker was in before
						he initiated the attack. 
					</p>
					<p>
					For more information on defence structures, <a href='odp/mechanics.jsp#defencestructures'>visit the game mechanics page</a>.
					</p>
				</div>
				<script type="text/javascript">updateMinimizeBox("#defenceStructureMinimizeButton", "#defenceStructureBox")</script>
				<h4><a id='defenceStructureMinimizeButton' onclick='toggleMinimizeBox(event, "#defenceStructureBox");' class=''>&#8711;</a> Defensive Structure
					<span class='hint' rel='#defencestructures' style='float:right'><img src='https://initium-resources.appspot.com/images/ui/help.png' border=0 style='max-height:19px;'/></span>			
				</h4>
				<div>Structural Integrity: ${defenceStructureHitpoints}/${defenceStructureMaxHitpoints}</div>
				<p>${statusDescription}</p>
				
					<div class='smallbox'>
						Current Leader
						<p>
						<c:if test='${leader==null}'>
							None
						</c:if>
						<c:if test='${leader!=null }'>
							<%=GameUtils.renderCharacter(null, leader) %>
						</c:if>
						</p>
					</div>
				
				<c:if test='${isLeader}'>
				
					<div class='smallbox'>
					<h5>Defence Structure Controls</h5>
						
					Defence Mode
					<div class='main-item-controls'>
						<a onclick='setBlockadeRule("BlockAllParent")' class='${defenceModeBlockAllParent}'>Blockade&nbsp;Everything</a>
						<a onclick='setBlockadeRule("BlockAllSelf")' class='${defenceModeBlockAllSelf}'>Defend&nbsp;Structure&nbsp;Only</a>
						<a onclick='setBlockadeRule("None")' class='${defenceModeNone}'>No&nbsp;Defence</a>
					</div>
					</div>
				
				</c:if>
				<h5 class='hint' rel='#defenders'>Active Defenders </h5>
				<div class='main-item-controls'>
					<a class='clue' rel='viewdefendersmini.jsp'>View Defenders</a>
				</div>
				<br>
				<div style='text-align:center'>
				<div class='smallbox'>
					1st Line
					<p><span class='hint' rel='#engagedDefenders'>${defenderEngaged1Count}</span>/<span class='hint' rel='#defenderCount'>${defender1Count}</span></p>
					<c:if test="${characterStatus!='Defending1'}">
						<p><a onclick='enterDefenceStructureSlot("Defending1")'>Join</a></p>
					</c:if>
					<c:if test="${characterStatus=='Defending1'}">
						<p class='hint' rel='#joinedDefenderGroup'>Joined</p>
					</c:if>
				</div>
				<div class='smallbox'>
					2nd Line
					<p><span class='hint' rel='#engagedDefenders'>${defenderEngaged2Count}</span>/<span class='hint' rel='#defenderCount'>${defender2Count}</span></p>
					<c:if test="${characterStatus!='Defending2'}">
						<p><a onclick='enterDefenceStructureSlot("Defending2")'>Join</a></p>
					</c:if>
					<c:if test="${characterStatus=='Defending2'}">
						<p class='hint' rel='#joinedDefenderGroup'>Joined</p>
					</c:if>
				</div>
				<div class='smallbox'>
					3rd Line
					<p><span class='hint' rel='#engagedDefenders'>${defenderEngaged3Count}</span>/<span class='hint' rel='#defenderCount'>${defender3Count}</span></p>
					<c:if test="${characterStatus!='Defending3'}">
						<p><a onclick='enterDefenceStructureSlot("Defending3")'>Join</a></p>
					</c:if>
					<c:if test="${characterStatus=='Defending3'}">
						<p class='hint' rel='#joinedDefenderGroup'>Joined</p>
					</c:if>
				</div>
				<div class='smallbox'>
					Not Defending
					<p>${notDefendingCount}</p>
					<c:if test="${characterStatus!=null}">
						<p><a onclick='enterDefenceStructureSlot("Normal")'>Join</a></p>
					</c:if>
					<c:if test="${characterStatus==null}">
						<p class='hint' rel='#joinedDefenderGroup'>Joined</p>
					</c:if>
				</div>
				</div>
			</div>
		</c:if>

		<div id='instanceRespawnWarning'></div>
		
		<c:if test='${(clientDescription!=null &&clientDescription!="") || (midMessage!=null && midMessage!="") || (prefix!=null && prefix!="")}'>
		<div class='main-dynamic-content-box paragraph'>
			<c:if test='${clientDescription!=null && clientDescription!="" && clientDescription!="null"}'>
			${clientDescription}
			</c:if>
			<c:if test='${midMessage!=null && midMessage!="" && midMessage!="null"}'>
			${midMessage}
			</c:if>
		</div>
		</c:if>

		<div class='main-dynamic-content-box' id='collectables-area'>
		</div>
		
		<div id='locationDescription' class='paragraph'>
			${locationDescription}
		</div>
		<c:if test="${combatSite==true}">
			<div class='boldbox'>
				<div id='inline-items' class='main-splitScreen'>
				</div>
				<div id='inline-characters' class='main-splitScreen'>
				</div>
			</div>
		</c:if>
		<c:if test="${supportsCamps!=null && supportsCamps>0}">
			<div class='main-description'>
				This location could host up to ${supportsCamps} camps.
			</div>
		</c:if>
		<c:if test="${supportsCamps==null || supportsCamps==0}">
			<div class='main-description'>
				This location is not suitable for a camp.
			</div>
		</c:if>		
		<%		
			if (monsterCount!=null && maxMonsterCount!=null)
			{
				if ("CampSite".equals(location.getProperty("type")))
				{
					if (monsterCount<1) monsterCount = 0d;
					{
						double monsterPercent = monsterCount/maxMonsterCount;
						out.println("<p>Camp integrity: <span class='main-item-subnote'>"+GameUtils.formatPercent(1d-monsterPercent)+"</span></p>");
						
					}
					
				}
				else
				{
					if (maxMonsterCount>10)
					{
						if (monsterCount<1) monsterCount = 0d;
						double monsterPercent = monsterCount/maxMonsterCount;
						out.println("<div class='main-description'>");
						out.println("The monster activity in this area seems ");
						if (monsterPercent>0.75)
							out.println("high compared to usual.");
						else if (monsterPercent>0.50)
							out.println("moderate compared to usual.");
						else if (monsterPercent>0.25)
							out.println("low compared to usual.");
						else if (monsterPercent>0)
							out.println("very low compared to usual.");
						else
							out.println("to be none.");
						
						//out.println("Debug: "+monsterCount+"/"+maxMonsterCount);
						out.println("</div>");
					}
				}
				
			}
		%>
		<div id='buttonBar'>${buttonBar}</div>
		<div class='main-splitScreen'>
			<div id='main-merchantlist'>
				<div class='main-button-half' onclick='loadLocationMerchants()' shortcut='83'>
 					<span class='shortcut-key'> (S)</span><img src='https://initium-resources.appspot.com/images/ui/magnifying-glass.png' border=0/> Nearby stores
				</div>
			</div>
		</div>
		<div id='partyPanel' class='main-splitScreen'>
		${partyPanel}
		</div>
		<div></div>
		<div class='main-splitScreen'>
			<div id='main-itemlist'>
				<div class='main-button-half' onclick='loadLocationItems()' shortcut='86'>
 					<span class='shortcut-key'> (V)</span><img src='https://initium-resources.appspot.com/images/ui/magnifying-glass.png' border=0/> Nearby items
				</div>
			</div>
		</div>
		<div class='main-splitScreen'>
			<div id='main-characterlist'>
				<div class='main-button-half' onclick='loadLocationCharacters()' shortcut='66'>
 					<span class='shortcut-key'> (B)</span><img src='https://initium-resources.appspot.com/images/ui/magnifying-glass.png' border=0/> Nearby characters
				</div>
			</div>
		</div>
		<div class='main-buttonbox'>
		
		<div id='locationScripts'>${locationScripts}</div>
		
		
		<div id='main-button-list'>${mainButtonList}</div>
		
		</div>
	</div>
	<div class='mobile-spacer'></div>

	<c:if test="${isThrowawayInSession==true}">
		<p id='throwawayWarning' class='highlightbox-red' style='position:fixed; bottom:0px;z-index:9999999; left:0px;right:0px; background-color:#000000;'>
			WARNING: Your throwaway character ${throwawayName} associated with this browser could be destroyed at any time! <a href='signup.jsp?convertThrowaway=true'>Click here to convert your character to a full account. It's free!</a><br>
			<br>
			Alternatively, you can <a onclick='destroyThrowaway()'>destroy your throwaway character</a>.
		</p>
	</c:if>
</body>
</html>