// App.tsx
import Board from "./Board";
import { sendGETRequest } from '@common/restAPI';
import { loadCommonConfig } from '@common/config';
import { useEffect, useState } from "react";

function App() {
  const [isConfigLoaded, setConfigLoaded] = useState<boolean>(false);
  const [areBoardsLoaded, setBoardsLoaded] = useState<boolean>(false);
  const [boardString, setBoardString] = useState<string>("");
  const [solution, setSolution] = useState<string>("");
  const [name, setName] = useState<string>("");
  const [level, setLevel] = useState<number>(0);
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
    level: number
  } 

  const handleInit = ( jsonResp: any ) => {    
    console.log("Number of boards  : ", jsonResp.boards.length );
    //console.log("Number of boards  : ", jsonResp );
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
          level: b.level
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
      <div className="auth-buttons">
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
            if( adminMode )
              console.log(boardString);
          }}
        >
          Save
        </button>
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
        adminMode={adminMode}
        startTimer={startTimer}
        setStartTimer={setStartTimer}
      />
      }
    </div>
  );
}
export default App;