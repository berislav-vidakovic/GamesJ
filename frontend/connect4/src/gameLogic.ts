import type { Dispatch, SetStateAction } from "react";
import { sendPOSTRequest } from '@common/restAPI';
import { StatusCodes } from "http-status-codes";

let setMyColorRef: Dispatch<SetStateAction<"Red" | "Yellow" | null>>;
let setGameStateRef: Dispatch<SetStateAction<"init" | "myMove" | "theirMove" | "draw" | "myWin" | "theirWin" | null>>;
let setBoardRowsRef: Dispatch<SetStateAction<string[]>>;


let myUserId : number | null = null;
let partnerId : number | null = null;
let myGameId : string | null = null;

export function updateUserIds(userId: number | null, user2Id: number | null,
    gameId: string | null = null ){
  myUserId = userId;
  partnerId = user2Id;
  myGameId = gameId;
  console.log("===============User IDs updated: ", myUserId, partnerId);
}

export function updateSetBoardRows( setBoardRows: Dispatch<SetStateAction<string[]>> ){
  setBoardRowsRef = setBoardRows;
}

export function setStateFunctionRefs(
  setMyColor: Dispatch<SetStateAction<"Red" | "Yellow" | null>>,
  setGameState: Dispatch<SetStateAction<"init" | "myMove" | "theirMove" | "draw" | "myWin" | "theirWin" | null>>
){
    setMyColorRef = setMyColor;
    setGameStateRef = setGameState;
}


export function stringToMatrix( boardString: string,  
    setBoardRows: Dispatch<SetStateAction<string[]>> ){
  const matrix: string[] = [];
  for( let i = 0; i < 6; i++ ){
    const row: string = boardString.slice(i*7,i*7+7);
    matrix.push( row );
  }
  setBoardRows(matrix.reverse());
  //console.log(" --------- stringToMatrix ----------------");
}

export function reConnect(){
  console.log("Reconnecting WebSocket...");
  // Implement WebSocket reconnection logic here
  sessionStorage.setItem("reconnecting", "true");
  sessionStorage.setItem("gameId", String(myGameId));
  sessionStorage.setItem("senderId", String(myUserId));  
  window.location.reload();
}

// -------------swapColors - POST request, POST response, WS incoming ----------
export async function swapColors( 
    gameId: string | null ){
  // /api/games/connect4/swapcolors
  // 1- send POST
  // 2- handle POST
  // 3- handle WS
  const body = JSON.stringify({gameId, userId: myUserId});
  sendPOSTRequest( 'api/games/connect4/swapcolors', body, handleSwapColorsResponse);
}

// Response { color }
async function handleSwapColorsResponse( jsonResp: any, status: number ) {
  if( status == StatusCodes.OK ){
    setMyColorRef(jsonResp.color);
  }
  else 
    alert(`Error: ${jsonResp.error} STATUS: ${status}`);
}

// ------------------new Game -----------------------------------------
export async function newGame(gameId: string | null){
  const body = JSON.stringify({gameId, userId: myUserId, user2Id: partnerId });
  console.log("Restart: ", myUserId, partnerId, body );
  
  sendPOSTRequest( 'api/games/connect4/newgame', body, handleNewGameResponse);
}

async function handleNewGameResponse( jsonResp: any, status: number ) {
  console.log("jsonResp", jsonResp, status);
  //setBoardStringRef(jsonResp.board);
  setGameStateRef("init");
  stringToMatrix(jsonResp.board, setBoardRowsRef);  
}

function handleWsNewGame( jsonData: any ){
  console.log("WS-jsonData", jsonData );
  //setBoardStringRef(jsonData.board);
  setGameStateRef("init");
  stringToMatrix(jsonData.board, setBoardRowsRef);   

}

// -------------startGame - POST request, POST reposne, WS incoming ----------
export async function startGame( 
    gameId: string | null,
    gameState: "init" | "myMove" | "theirMove" | "draw" | "myWin" | "theirWin" | null
  ){
  console.log( "GAME STATE: ", gameState );
  if( gameState == "init") {
    const body = JSON.stringify({gameId, userId: myUserId });
    console.log("POST body: ", body);
    sendPOSTRequest( 'api/games/connect4/start', body, handleStartGameResponse);
  }
  //else
    //restartGame(gameId);
}
// Response to POST message
async function handleStartGameResponse( jsonResp: any, status: number ) {
  // Response: { userId, board }
  if( status == StatusCodes.OK ){
    //console.log("jsonResp.userId", jsonResp.userId, myUserId)
    if( Number(jsonResp.userId) == myUserId )
      setGameStateRef( "myMove");
    else
      setGameStateRef( "theirMove");
    //console.log("Board POST: ", jsonResp.board);
    stringToMatrix(jsonResp.board, setBoardRowsRef);   
  }
  else if( status == StatusCodes.ACCEPTED ) { // 202
    alert( "User 2 did not open game window");
  }
  else 
    alert(`Error: ${jsonResp.error} STATUS: ${status}`);
}

// WS message sent to Game partner
function handleWsStartGame( jsonData: any ){
  // Response: { userId, board }   
  //console.log("jsonMsg.data.userId", jsonMsg.data.userId, myUserId)
  if( Number(jsonData.userId) == myUserId )
    setGameStateRef( "myMove");
  else
    setGameStateRef( "theirMove");
  stringToMatrix(jsonData.board, setBoardRowsRef);      
  //console.log("Board WS: ", jsonMsg.data.board);
}



// -------------insertDisk - POST request, POST reposne, WS incoming ----------
export async function insertDisk( 
    gameId: string | null, row: number, col: number ){
  const body = JSON.stringify({gameId, userId: myUserId, row, col });
  sendPOSTRequest( 'api/games/connect4/insertdisk', body, handleInsertDiskResponse);
}

async function handleInsertDiskResponse( jsonResp: any, status: number ) {
  if( status == StatusCodes.OK ){    // { userId, board, state }
    stringToMatrix(jsonResp.board, setBoardRowsRef);   
    if( jsonResp.state=="inprogress")
      setGameStateRef( "theirMove");
    else if( jsonResp.state =="gameover")
      console.log(" *** Game Over *** ");
  }
  else 
    alert(`Error: ${jsonResp.error} STATUS: ${status}`);
}

// ------------- WS message handlers -----------------------------------
export async function handleWsMessage( jsonMsg: any ) {
    if( jsonMsg.type== "swapColors" )
      setMyColorRef(jsonMsg.data.color);
    else if( jsonMsg.type== "startGame" ) 
      handleWsStartGame( jsonMsg.data );
    else if( jsonMsg.type == "insertDisk" ) {
      stringToMatrix(jsonMsg.data.board, setBoardRowsRef);
      if( jsonMsg.data.state=="inprogress")
        setGameStateRef( "myMove");
      else if( jsonMsg.data.state =="gameover")
        console.log(" *** WS *** Game Over ***");
    }
    else if( jsonMsg.type == "gameOver" ) {
      //console.log("WS GameOver received");
      if( jsonMsg.data.result == "draw" )
        setGameStateRef( "draw");
      else if( jsonMsg.data.result == "win" ){
        if( jsonMsg.data.userId == myUserId )
          setGameStateRef( "myWin");
        else
          setGameStateRef( "theirWin");
      }
    }
    else if( jsonMsg.type == "newGame" ) 
      handleWsNewGame( jsonMsg.data );    
}
