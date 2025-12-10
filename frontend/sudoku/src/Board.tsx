import React, { useEffect, useRef, useState, type Dispatch, type SetStateAction } from "react";
import "@common/style.css";
import '@common/style-mobile.css';

const okSound = new Audio("sounds/OK.wav");  
const nokSound = new Audio("sounds/NOK.mp3"); 

interface SudokuBoardProps {
  boardString: string;
  setBoardString: Dispatch<SetStateAction<string>>;
  solutionString: string;
  name: string;
  level: number;
  testedOK: boolean;
  adminMode: boolean;
  startTimer: boolean;
  setStartTimer: Dispatch<SetStateAction<boolean>>;
}

type BoardArray = string[][];

const SudokuBoard: React.FC<SudokuBoardProps> = ({ 
  boardString, setBoardString, solutionString, name, level, testedOK, adminMode, startTimer, setStartTimer }) => {
  

  const initialBoard: BoardArray = Array.from({ length: 9 }, (_, row) =>
    Array.from({ length: 9 }, (_, col) => boardString[row * 9 + col])
  );

  const solutionBoard: BoardArray = Array.from({ length: 9 }, (_, row) =>
    Array.from({ length: 9 }, (_, col) => solutionString[row * 9 + col])
  );

  const [board, setBoard] = useState<BoardArray>(initialBoard);
  const [focused, setFocused] = useState<[number, number] | null>(null);
  const [message, setMessage] = useState<string>("");
  const [errorCells, setErrorCells] = useState<(string | null)[][]>(
    Array.from({ length: 9 }, () => Array(9).fill(null))
  );
  const [time, setTime] = useState<number>(0); // seconds
  const [mistakes, setMistakes] = useState<number>(0);
  const boardRef = useRef<HTMLDivElement>(null);

  // Auto-focus
  useEffect(() => {
    boardRef.current?.focus();
  }, []);

  // Timer controlled by parent
  useEffect(() => {
    if (!startTimer) {
      setTime(0);
      return;
    }
    setFocusFirstEmpty();
    const timer = setInterval(() => setTime(prev => prev + 1), 1000);
    return () => clearInterval(timer);
  }, [startTimer]);   // start only when parent sets it true

  useEffect(() => {
    if (startTimer) {
      boardRef.current?.focus();  
    }
  }, [startTimer]);
 

  function setFocusFirstEmpty(){
    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        if (board[r][c] === "0") {
          setFocused([r, c]);
          console.log("Focus set ", r, c);
          return;
        }
      }
    }
  }
  // Focus first empty cell
  useEffect(() => {
    if( adminMode )
      setFocused([
        Number(sessionStorage.getItem("lastRow")), 
        Number(sessionStorage.getItem("lastCol")) ]);
    else
      setFocusFirstEmpty();
  }, []);

  const boardStrFrom2Darray = (arr2D: string[][]) => {
    //console.log(arr2D);
    let s: string = "";
    for( let i = 0; i < arr2D.length; i++ )
      s += arr2D[i].join('');
    return s;
  } 

  function evaluateGame( goodGuess: boolean, board: string, mistakes: number ){
    console.log(board);
    if( !board.includes("0") ) {
      console.log(time);
      setMessage("CONGRATULATIONS!!! 😊");
      setStartTimer(false);

    }
    else if( mistakes == 5 ){
      console.log(time);

      setMessage("GAME OVER 😢");
      setStartTimer(false);
    }
    else if ( goodGuess )
      setMessage("Good guess ✅");
    else
      setMessage("Wrong guess ❌");
  }

  const moveFocus = (dr: number, dc: number) => {
    if (!focused) return;
    const [r0, c0] = focused;

    let newR = (r0 + dr);
    let newC = (c0 + dc);

    newR = Math.min(newR, 8);
    newC = Math.min(newC, 8);
    
    newR = Math.max(newR, 0);
    newC = Math.max(newC, 0);

    setFocused([newR, newC]);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
    if (!focused) return;
    const [r, c] = focused;

    switch (e.key) {
      case "ArrowUp":
        e.preventDefault();
        moveFocus(-1, 0);
        break;
      case "ArrowDown":
        e.preventDefault();
        moveFocus(1, 0);
        break;
      case "ArrowLeft":
        e.preventDefault();
        moveFocus(0, -1);
        break;
      case "ArrowRight":
        e.preventDefault();
        moveFocus(0, 1);
        break;
      default:
        if( adminMode && (/^[1-9]$/.test(e.key) || e.key == " " ) ){
          console.log("admin mode");
          const newBoard = board.map(row => [...row]);
          newBoard[r][c] = e.key == " " ? "0" : e.key;
          setBoard(newBoard);
          setBoardString(boardStrFrom2Darray(newBoard));
          sessionStorage.setItem("lastRow", String(r));
          sessionStorage.setItem("lastCol", String(c));
        }
        else if ( /^[1-9]$/.test(e.key) && (board[r][c] === "0" || errorCells[r][c]) ) {
          if (!startTimer) return;
          const newBoard = board.map(row => [...row]);
          const newErrors = errorCells.map(row => [...row]);

          if (e.key === solutionBoard[r][c]) {
            newBoard[r][c] = e.key;
            newErrors[r][c] = null;
            evaluateGame(true, boardStrFrom2Darray(newBoard), mistakes);
            okSound.currentTime = 0; 
            okSound.play(); 
          } else {
            newBoard[r][c] = "0";
            newErrors[r][c] = e.key;
            setMistakes(prev => prev + 1);
            evaluateGame(false, boardStrFrom2Darray(newBoard), mistakes+1);           
            nokSound.currentTime = 0; // rewind in case it's still playing
            nokSound.play();
          }

          setBoard(newBoard);
          setErrorCells(newErrors);
        }
    }
  };

  // Format time mm:ss
  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60).toString().padStart(2, "0");
    const s = (seconds % 60).toString().padStart(2, "0");
    return `${m}:${s}`;
  };

  const handleNumberClick = (num: string) => {
    if (!focused) return;
    const [r, c] = focused;

    // Only accept empty or error cells
    if (board[r][c] !== "0" && !errorCells[r][c]) return;

    const newBoard = board.map(row => [...row]);
    const newErrors = errorCells.map(row => [...row]);

    if (num === solutionBoard[r][c]) {
      newBoard[r][c] = num;
      newErrors[r][c] = null;
      evaluateGame(true, boardStrFrom2Darray(newBoard), mistakes);
      okSound.currentTime = 0;
      okSound.play();
    } else {
      newBoard[r][c] = "0";
      newErrors[r][c] = num;
      setMistakes(prev => prev + 1);
      evaluateGame(false, boardStrFrom2Darray(newBoard), mistakes+1);
      nokSound.currentTime = 0;
      nokSound.play();
    }

    setBoard(newBoard);
    setErrorCells(newErrors);
  };


  return (
    <>
      {/* Name and level */}
      <div className={"sudokuinfobox"}  >
        <div>Game: {name}</div>
        { (
        <div>{
          !testedOK  
          ? <span style={{ color: "red"}}>❌ not tested</span>
          : <span style={{ color: "green"}}>✅ Tested OK</span>          
        }
        </div>)
        }
        <div>Level: {level == 2 ? "Medium" : "Easy"}</div>
      </div>
      
      {/* Timer and Mistakes */}
      <div className={"sudokuinfobox"} style={{fontWeight:"600"}}>
        <div>Timer: {formatTime(time)}</div>
        <div>{message}</div>
        <div>Mistakes: {mistakes}</div>
      </div>

      <div
        className="sudoku-board"
        tabIndex={0}
        ref={boardRef}
        onKeyDown={handleKeyDown}
      >
        {board.map((row, r) =>
          row.map((cell, c) => {
            const isFocused = focused?.[0] === r && focused?.[1] === c;
            const wrongNumber = errorCells[r][c];

            // Highlight row, column, or box
            const inSameRow = focused?.[0] === r;
            const inSameCol = focused?.[1] === c;
            const inSameBox =
              focused &&
              Math.floor(focused[0] / 3) === Math.floor(r / 3) &&
              Math.floor(focused[1] / 3) === Math.floor(c / 3);

            const classes = [
              "sudoku-cell",
              (inSameRow || inSameCol || inSameBox) && !isFocused ? "highlight" : "",
              wrongNumber ? "error" : "",
              isFocused ? "focused" : "",
              (r + 1) % 3 === 0 && r !== 8 ? "border-bottom" : "",
              (c + 1) % 3 === 0 && c !== 8 ? "border-right" : "",
            ]
              .filter(Boolean)
              .join(" ");


            return (
              <div
                key={`${r}-${c}`}
                className={classes}
                onClick={() => {
                  // Allow focusing only empty or error cells
                  if (board[r][c] === "0" || errorCells[r][c]) {
                    setFocused([r, c]);
                    boardRef.current?.focus(); // ensure keyboard input works
                  }
                }}
              >
                {cell !== "0" ? cell : wrongNumber ? wrongNumber : ""}
              </div>
            );
          })
        )}
      </div>
      <div className="sudoku-numpad">
        {[1,2,3,4,5,6,7,8,9].map(num => (
          <button
            key={num}
            className="sudoku-numpad-button"
            onClick={() => handleNumberClick(num.toString())}
          >
            {num}
          </button>
        ))}
      </div>
    </>
  );
};

export default SudokuBoard;
