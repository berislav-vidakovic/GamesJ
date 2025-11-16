//import { StatusCodes } from "http-status-codes"
import type { Dispatch, SetStateAction } from "react";
import type { User } from '@common/interfaces';
import { StatusCodes } from "http-status-codes";
import { URL_CONNECT4 } from '@common/config';


let setInitializedRef:  Dispatch<SetStateAction<boolean>>;
let setUsersRegisteredRef:  Dispatch<SetStateAction<User[]>>;
let setCurrentUserIdRef:  Dispatch<SetStateAction<number | null>>; 
let setOnlineUsersRef:  Dispatch<SetStateAction<number>>;
let setCallerUserIdRef:  Dispatch<SetStateAction<number | null>>;
let setCalleeUserIdRef:  Dispatch<SetStateAction<number | null>>;
let setInvitationStateRef: Dispatch<SetStateAction<"init" | "sent" | "pending" | "paired">>;
let setSelectedGameRef: Dispatch<SetStateAction<"panel.game.sudoku" | "panel.game.connect4" | "panel.game.memory" | null>>;
let setTechStackRef: Dispatch<SetStateAction<string[]>>;


export function setStateFunctionRefs(
  setInitialized:  Dispatch<SetStateAction<boolean>>,
  setUsersRegistered:  Dispatch<SetStateAction<User[]>>,
  setCurrentUserId:  Dispatch<SetStateAction<number | null>>,
  setOnlineUsers:  Dispatch<SetStateAction<number>>,
  setCallerUserId:  Dispatch<SetStateAction<number | null>>,
  setCalleeUserId:  Dispatch<SetStateAction<number | null>>,
  setInvitationState: Dispatch<SetStateAction<"init" | "sent" | "pending" | "paired">>,
  setSelectedGame: Dispatch<SetStateAction<"panel.game.sudoku" | "panel.game.connect4" | "panel.game.memory" | null>>,
  setTechStack: Dispatch<SetStateAction<string[]>>
){
    setInitializedRef = setInitialized;
    setUsersRegisteredRef = setUsersRegistered;
    setCurrentUserIdRef = setCurrentUserId;
    setOnlineUsersRef = setOnlineUsers;
    setCallerUserIdRef = setCallerUserId;
    setCalleeUserIdRef = setCalleeUserId;
    setInvitationStateRef = setInvitationState;
    setSelectedGameRef = setSelectedGame;
    setTechStackRef = setTechStack;
}

export  const handleResponseGetAllUsers = ( jsonResp: any ) => {    
  // Map API response fields to match your User interface
  const mappedUsers: User[] = jsonResp.users.map((u: any) => ({
    userId: u.userId,
    login: u.login,
    fullname: u.fullName,  
    isonline: u.isOnline   
  }));
  const onlineusers = mappedUsers.filter( u => u.isonline == true ).length;
  //console.log("Online user(s):", onlineusers );
  setOnlineUsersRef( onlineusers );

  // Update React state - ref. to setUsersRegistered 
  setUsersRegisteredRef(mappedUsers);
  //console.log("Response to GET users: ", jsonResp );
  sessionStorage.setItem("myID", jsonResp.id );
  setInitializedRef(true);
  setCurrentUserIdRef(null);
  setTechStackRef(jsonResp.techstack);
  console.log("Tech stack:", jsonResp.techstack);
}

export const handleResponseSignUp = ( jsonResp: any, status: number ) => {    
  console.log("*** HANDLE User registered: ", jsonResp, "Status: ", status);
  if( jsonResp.acknowledged ) {     
    console.log("User registered: ", jsonResp.user);
  }
  else {
    console.log("User NOT registered: ", jsonResp.error);
    alert("NOT registered: User already exists");
  }
}

export function handleUserLogin( jsonResp: any, status: number ){
  // Response: { userId, isOnline, accessToken, refreshToken }
  console.log("******** ****** POST response handleUserLogin received: ", 
      jsonResp, "Status: ", status); 
  if( status == StatusCodes.OK ){
    setCurrentUserIdRef(Number(jsonResp.userId));
    sessionStorage.setItem("accessToken", jsonResp.accessToken);
    sessionStorage.setItem("refreshToken", jsonResp.refreshToken);
    sessionStorage.setItem("userId", jsonResp.userId.toString());
    console.log("Login OK", jsonResp);
  }
}


export function handleUserLogout( jsonResp: any, status: number ){
  console.log("Logout POST response received: ", jsonResp); 
  //var response = new { userId, isOnline = false };  
  if( status == StatusCodes.OK ) {
    setCurrentUserIdRef(null);
    sessionStorage.removeItem("accessToken");
    sessionStorage.removeItem("refreshToken");
    sessionStorage.removeItem("userId");
  }
}

// Request { "callerId": int, "calleeId": int, "selectedGame": "panel.game.connect4" } 
// -> Response { "invitation": "send" | "cancel" | "accept" | "reject", callerId, calleeId, selectedGame }
export function handleInvite( jsonResp: any, status: number ){
  if( status == StatusCodes.OK ){
    switch( jsonResp.invitation ){
      case "send":
        setCallerUserIdRef( Number(jsonResp.callerId) );
        setCalleeUserIdRef( Number(jsonResp.calleeId) );
        setInvitationStateRef( "sent" );
        break;
      case "cancel":
        setCallerUserIdRef( null );
        setCalleeUserIdRef( null );
        setInvitationStateRef( "init" );
        break;
      case "accept":
        setCallerUserIdRef( Number(jsonResp.callerId) );
        setCalleeUserIdRef( Number(jsonResp.calleeId) );
        setInvitationStateRef( "paired" );
        break;
      case "reject":
        setCallerUserIdRef( null );
        setCalleeUserIdRef( null );
        setInvitationStateRef( "init" );
        break;
    }
  }
  else 
    alert(`Error: ${jsonResp.error} STATUS: ${status}`);
}

// Req: { run: "Connect Four", userId1, userId2, senderId, refreshToken } 
// ->Resp: { game: "Connect Four", gameid, senderId, refreshToken, accessToken }
export function handleResponseRunGame( jsonResp: any, status: number ){
  console.log("******** ****** POST response handleResponseRunGame received: ", 
      jsonResp, "Status: ", status);
  sessionStorage.setItem("refreshToken", jsonResp.refreshToken);
  sessionStorage.setItem("accessToken", jsonResp.accessToken);
  // encodeURIComponent() protects URL from breaking when values contain 
  // characters that are not allowed inside a URL parameter (like ? and & )
  const urlNewBrowser =
    `${URL_CONNECT4}?` +
    `gameId=${encodeURIComponent(jsonResp.gameid)}&` +
    `senderId=${encodeURIComponent(jsonResp.senderId)}&` +
    `accessToken=${encodeURIComponent(jsonResp.accessToken)}&` +
    `refreshToken=${encodeURIComponent(jsonResp.refreshToken)}`;
  console.log("Opening new game window URL:", urlNewBrowser );
  if( status == StatusCodes.OK )
    if( jsonResp.game == "panel.game.connect4" )    
      window.open(urlNewBrowser, '_blank');    
}

// ws message handlers -----------------------------------
export async function handleWsMessage( jsonMsg: any ) {
  ////console.log("WS conn:", isWsConnected );
    if( jsonMsg.type== "userRegister" )
      handleWsUserRegister(jsonMsg.data);
    else if( jsonMsg.type == "userSessionUpdate" )
      handleWsUserSessionUpdate(jsonMsg.data)
    else if( jsonMsg.type == "invitation" )
      handleWsInvitation(jsonMsg.data)
}

function handleWsInvitation( jsonResp: any ){
  // Request { "callerId": int, "calleeId": int, "selectedGame": "panel.game.connect4" } 
  // -> Response { "invitation": "send" | "cancel" | "accept" | "reject", callerId, calleeId, selectedGame }  
  console.log("Received WS invitation: ", jsonResp);
  switch( jsonResp.invitation){
    case "send":
      setCallerUserIdRef( Number(jsonResp.callerId) );
      setCalleeUserIdRef( Number(jsonResp.calleeId) );
      setInvitationStateRef("pending");
      setSelectedGameRef(jsonResp.selectedGame);
      console.log("WS invitation ", jsonResp );
      break;
    case "cancel":
    case "reject":
    case "timeout":
      setCallerUserIdRef( null );
      setCalleeUserIdRef( null );
      setInvitationStateRef("init");
      break;
    case "accept":
      setCallerUserIdRef( Number(jsonResp.callerId) );
      setCalleeUserIdRef( Number(jsonResp.calleeId) );
      setInvitationStateRef( "paired" );
      break;
  }  
}


function handleWsUserRegister( jsonResp: any ){
  console.log("*** Ws-HANDLE User registered: ", jsonResp);
  if( jsonResp.acknowledged ) { 
    // Construct the new user object
    const newUser: User = {
      userId: jsonResp.user.userId,
      login: jsonResp.user.login,
      fullname: jsonResp.user.fullName,
      isonline: jsonResp.user.isOnline,
    };
    // Update frontend state (append to existing users list)    
    setUsersRegisteredRef(prev => {
      const dupe = prev.some(u => u.userId === newUser.userId);
      //if( dupe ) //console.log("=====================Duplicate ID found, no user appending");
      return dupe ? prev : [...prev, newUser];
    });

    //console.log("Ws-User registered: ", jsonResp.user);
  }
  else {
    //console.log("User NOT registered: ", jsonResp.error);
    alert("NOT registered: User already exists");
  }
}

async function handleWsUserSessionUpdate( jsonMsgData: any ) {
  //var response = new { userId, isOnline = true, automaticLogout (optional) };
  //var msg = new { type = "userSessionUpdate", status = "WsStatus.OK", data = response };
  const userId = jsonMsgData.userId;
  const isOnline = jsonMsgData.isOnline;    
    setUsersRegisteredRef(prev => {
    const updated = prev.map(u =>
      u.userId === userId
        ? { ...u, isonline: isOnline } // update online status
        : u
    );
    // compute online users count from the updated array
    const onlineCount = updated.filter(u => u.isonline).length;
    setOnlineUsersRef(onlineCount);    
    //console.log("Online user(s):", onlineCount);
    return updated;
  });
  // Auto-logout
  if( jsonMsgData.automaticLogout && userId == sessionStorage.getItem("userId") ) {
    console.log( "Automatic logout ........ ");
    setCurrentUserIdRef(null)
  }
}


