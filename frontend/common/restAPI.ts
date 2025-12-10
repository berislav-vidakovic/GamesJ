import { StatusCodes } from "http-status-codes"
import  { URL_BACKEND_HTTP }  from './config';

// Generic GET sending
export async function sendGETRequest(endpoint: string, handleResponse: (data: any) => void ): Promise<any> {
    //const getUrl = `${URL_BACKEND_HTTP}/${endpoint}`;
    const getUrl = `${URL_BACKEND_HTTP}/${endpoint}` + `?id=${sessionStorage.getItem("myID")}`;
    console.log("Sending GET: ", getUrl );

    fetch(getUrl, { 
        method: "GET",
        headers: {
          "Authorization": "Bearer " + sessionStorage.getItem("accessToken"),
          "Content-Type": "application/json"
        },
        credentials: "include" // required for cookies
      } ) 
      .then( async(res) => { // this returns a Promise that resolves with parsed JSON
        if (!res.ok) { 
          const errorText = await res.text();
          throw new Error(`${res.status}: ${errorText || res.statusText}`);
        }
        //console.log("...received GET response!");
        return res.json();
      })
      .then( (jsonResp) => {          
          handleResponse( jsonResp );
      } )  // runs after parsing completes
      .catch(err => console.error("GET request failed:", err));
}

// Generic POST sending
export async function sendPOSTRequest(
    endpoint: string, 
    msgBody: string, 
    handleResponse: (data: any, status: number) => void ): Promise<any> {
  const postUrl = `${URL_BACKEND_HTTP}/${endpoint}` + `?id=${sessionStorage.getItem("myID")}`;
  console.log("Sending POST: ", `${postUrl} Body:${msgBody}` );
  fetch( postUrl, {
          method: "POST",
          headers: { 
            "Authorization": "Bearer " + sessionStorage.getItem("accessToken"),
            "Content-Type": "application/json" 
          },
          body: msgBody, 
  }) 
    .then(async (res) => { 
      //console.log("...received POST response!");
      const jsonResp = await res.json();
      
      switch (res.status) {
        case StatusCodes.OK: // 200
        case StatusCodes.CREATED: // 201
        case StatusCodes.ACCEPTED: // 202
        case StatusCodes.RESET_CONTENT: // 205
        case StatusCodes.CONFLICT: // 409
        case StatusCodes.BAD_REQUEST: // 400      
        case StatusCodes.NOT_FOUND: // 404      
        case StatusCodes.UNAUTHORIZED: // 401 
        case StatusCodes.NO_CONTENT: // 204  - no body!
          handleResponse( jsonResp, res.status );
          break;
        default:
          throw new Error(`HTTP error! status: ${res.status}`);
      }
    })
    .catch(err => console.log("POST request failed:", err));
}



