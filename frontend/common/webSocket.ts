import { URL_BACKEND_WS } from './config';
import type { Dispatch, SetStateAction } from "react";

let ws : WebSocket | null = null;

export async function connectWS(
  setWsConnected:  Dispatch<SetStateAction<boolean>>,
  handleWsMessage: (jsonMsg: any) => void ) {
    try {
      if( ws === null || ws.readyState !== WebSocket.OPEN ) { 
        /*
        if( !sessionStorage.getItem("testBreak") ) {
          sessionStorage.setItem("myID", "123");
          sessionStorage.setItem("testBreak", true);
        }*/
        
        let myID = sessionStorage.getItem("myID");
        const params = new URLSearchParams();
        if (myID != null) 
          params.append('id', myID);
        //console.log("PARAMS: ", params.toString());
        const wsUrl = `${URL_BACKEND_WS}?${params.toString()}`;        
        ws = new WebSocket(wsUrl);
        
        //console.log("Creating new WebSocket connection...");

        ws.onopen = () => {
          console.log("WebSocket connected!");
          setWsConnected(true);
        };

        ws.onclose = (event) => {
          console.warn("WebSocket closed by server:", event.reason);
          setWsConnected(false);
        };

        ws.onerror = (error) => {
          console.error("WebSocket error:", error);          
          setWsConnected(false);
        };

        ws.onmessage = (event) => {
          //console.log("...received WS message");
          const jsonMsg = JSON.parse(event.data);
          handleWsMessage(jsonMsg);
        };
      }
      else  {
         //console.log("WS connected already");
         setWsConnected(true);
      }

    } catch (error) {
      console.error('Error establishing WS connection', error);
      setWsConnected(false);
    }
}

// WebSocket::readyState: CONNECTING: 0; OPEN: 1; CLOSING: 2; CLOSED: 3;
export async function sendWsMessage( strJson: string) : Promise<void> {
  if( ws !== null && ws.readyState === WebSocket.OPEN )
    ws.send(strJson);
}

