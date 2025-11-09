import battleshipImg from '../assets/battleships.gif';
import './App.css';
import "@common/style.css";
import '@common/style-mobile.css';

import { loadCommonConfig } from '@common/config';
import { useState, useEffect } from 'react';

function App() {
  const [isConfigLoaded, setConfigLoaded] = useState<boolean>(false);
  
  useEffect( () => { 
    loadCommonConfig(setConfigLoaded);    
    console.log(isConfigLoaded); 
  }, []);


  return (
    <div className="app-container">
      <h2>Battleships</h2>
      <div className="buttons-container">
        <button onClick={() => {
            //if( !isConfigLoaded )
              ////console.log("Config not loaded");
            //else  
             // //console.log("Battleships is under construction");
          }
        }>
          <img src={battleshipImg} alt="Battleships" />
        </button>        
      </div>
    </div>
  );
}

export default App;
