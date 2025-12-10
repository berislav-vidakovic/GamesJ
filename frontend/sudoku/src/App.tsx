// App.tsx
import Board from "./Board";
import { sendGETRequest, sendPOSTRequest } from '@common/restAPI';
import { loadCommonConfig } from '@common/config';
import { useEffect, useState } from "react";
import { StatusCodes } from "http-status-codes";

function App() {
  const [isConfigLoaded, setConfigLoaded] = useState<boolean>(false);
  const [areBoardsLoaded, setBoardsLoaded] = useState<boolean>(false);
  const [boardString, setBoardString] = useState<string>("");
  const [solution, setSolution] = useState<string>("");
  const [name, setName] = useState<string>("");
  const [level, setLevel] = useState<number>(0);
  const [testedOK, setTested] = useState<boolean>(false);
  const [adminMode, setAdminMode] = useState<boolean>(true);
  const [selectedGameIdx, setSelectedGameIdx] = useState<number | null>(null);
  const [games, setGames] = useState<Game[]>([]);
  const [startTimer, setStartTimer] = useState(false);  
  
  useEffect( () => { 
    loadCommonConfig(setConfigLoaded);     
    const params = new URLSearchParams(window.location.search);
    console.log( "Params: userId=", params.get('userId') );
  }, []);

   useEffect( () => { if( isConfigLoaded){
      sendGETRequest('api/sudoku/board', handleInit );
   }      
  }, [isConfigLoaded]);

  interface Game {
    board: string,
    solution: string, 
    name: string, 
    level: number,
    testedOK: boolean
  } 

  const handleInit = ( jsonResp: any ) => {    
    console.log("Boards  : ", jsonResp );
    //return;
    sessionStorage.setItem("myID", jsonResp.clientId );
    if( jsonResp.boards.length ){
      const games : Game[] = [];
      for( let i = 0; i < jsonResp.boards.length; i++ ){
        const b = jsonResp.boards[i];
        games.push({
          board: b.board,
          solution: b.solution,
          name: b.name,
          level: b.level,
          testedOK: b.testedOK
        });
      }
      setGames(games);
      //console.log(games);
      console.log("GAME count: ",  games.length);
    }
    setBoardsLoaded(true);
  }

  const setCurrentGame = (idx: number) => {
    setBoardString(games[idx].board);
    setSolution(games[idx].solution);
    setName(games[idx].name);
    setLevel(games[idx].level);
    setTested( games[idx].testedOK);

  }

  useEffect(() => {
    if (games.length > 0 && selectedGameIdx === null) {
      setSelectedGameIdx(0);
      setCurrentGame(0);
    }
  }, [games]);

  const createEmptyBoard = () => {
    setBoardString("0".repeat(81));
    setSolution("0".repeat(81)); // Or also empty for now
    setName("New Game");
    setLevel(1);
    setBoardsLoaded(true);
  };

  return (
    <div 
      style={{
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        paddingTop: "16px",
        backgroundColor: "#f8f9fa",
      }}>
      
      { adminMode && (
      <div 
        className="auth-buttons"
        style={{ display: "flex", flexDirection: "column", alignItems: "center" }}
      >      

      <div style={{ display: "flex", gap: "4px", justifyContent: "center", alignItems: "center" }}>
        <button
          onClick={()=>{
            setStartTimer(false); 
            setAdminMode(true);
            createEmptyBoard();
          }}
        >
          Add game
        </button>
        
        <button
         onClick={()=>{                          
              console.log(boardString); 
              sendPOSTRequest('api/sudoku/addgame', 
                JSON.stringify({ boardString, name }), 
              (jsonResp: any, status: number) => {
                switch(status)
                {
                  case StatusCodes.OK:
                    console.log("Added new game: ", jsonResp);
                    sessionStorage.setItem("currentGameAdding", boardString );
                    break;
                  default:
                      console.log("Error encountered: ", jsonResp);

                }
              });
            }
          }   
        >
          Board
        </button>

         {(<button
          disabled={false} //{boardString.includes("0")}}
          onClick={()=>{                          
              console.log(boardString); 
              const board = sessionStorage.getItem("currentGameAdding" );
              sendPOSTRequest('api/sudoku/solution', 
                    JSON.stringify({ board, solution: boardString, name }), 
              (jsonResp: any, status: number) => {
                  switch(status)
                  {
                    case StatusCodes.OK:
                      const game: Game = {
                        board: jsonResp.board,
                        solution: jsonResp.solution,
                        name: jsonResp.name,
                        testedOK: false,
                        level: 2
                      };
                      games.push(game);
                      console.log("Board solution updated: ", jsonResp);
                      break;
                    default:
                      console.log("Error encountered: ", jsonResp);
                  }
                });
            }
          }          
        >
          Solution
        </button>)        
        }
        </div>


          {
          <div style={{ width: "100%", marginTop: "8px" }}>
            <button
              onClick={()=>{                          
                  console.log(boardString, name, games[selectedGameIdx as number].name); 
                  sendPOSTRequest('api/sudoku/setname', 
                    JSON.stringify({ board: boardString, name }),  
                      (jsonResp: any, status: number) => {
                        switch(status)
                        {
                          case StatusCodes.OK:
                            const game = games.find(g=>g.board == jsonResp.board);
                            if( game ) {
                              game.name = jsonResp.name;
                              setGames([...games]);   // Trigger list refresh
                              console.log("Board name updated: ", jsonResp);
                            }
                            else  
                              console.log("Error - board not found", jsonResp);
                            break;
                          default:
                            console.log("Error encountered: ", jsonResp);
                        }
                      }
                  );
                }
              }            
            >Set Name</button>

            <input 
              type="text" 
              style={{ width: "30%", boxSizing: "border-box", marginLeft: "6px" }} 
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Game name"
            />

            <button
              onClick={()=>{                          
                  console.log(boardString, name, games[selectedGameIdx as number].name); 
                  sendPOSTRequest('api/sudoku/tested', 
                    JSON.stringify({ board: boardString, name }),  
                      (jsonResp: any, status: number) => {
                        switch(status)
                        {
                          case StatusCodes.OK:
                            const game = games.find(g=>g.board == jsonResp.board);
                            if( game ) {
                              game.name = jsonResp.name;
                              game.testedOK = true;        
                              setGames([...games]);   // Trigger list refresh
                              setTested(true);        // Update current game UI                      
                              console.log("Tested OK updated: ", jsonResp);
                            }
                            else  
                              console.log("Error - board not found", jsonResp);
                            break;
                          default:
                            console.log("Error encountered: ", jsonResp);
                        }
                      }
                  );
                }
              }  
            
            >Tested OK</button>

          </div>
          }


      </div>
      )}
      
      <h2>Sudoku</h2>
      <div className="auth-buttons">
        <button
          onClick={() => {
            console.log("selGame=", selectedGameIdx);
            setCurrentGame(selectedGameIdx as number);
            setAdminMode(false);
            setStartTimer(true);
          }}
        >
          Start
        </button>    

        <button
          onClick={()=>{
            //if( startTimer || adminMode ) return;
            if( startTimer  ) return;
            let idx:number = (selectedGameIdx as number+1) % games.length;
            console.log(selectedGameIdx, idx, games.length);
            setSelectedGameIdx(idx);
            setCurrentGame(idx);
          } }
        >
          Next
        </button>     
        <button
          onClick={()=> {
            if( !adminMode )
              setStartTimer(false); 
              setBoardsLoaded(false);
              sendGETRequest('api/sudoku/board', handleInit );

            //setRestartTimerFlag(!restartTimerFlag);
          } }
        >
          New game
        </button>    
      </div>
     
      {areBoardsLoaded && boardString.length === 81 && solution.length === 81 && 
      <Board 
        key={boardString}  // forces component to remount whenever board string changes
        boardString={boardString} 
        setBoardString={setBoardString}
        solutionString={solution} 
        name={name}
        level={level}
        testedOK={testedOK}
        adminMode={adminMode}
        startTimer={startTimer}
        setStartTimer={setStartTimer}
      />
      }
    </div>
  );
}
export default App;