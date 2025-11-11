// LoginDialog.tsx
import type { Dispatch, SetStateAction } from "react";
import type { User } from '@common/interfaces';
import { useRef, useEffect } from "react";
import { loginUser } from "../utils.ts";


function LoginDialog({ 
    setShowLoginDialog, usersRegistered, isWsConnected 
  }: { 
    setShowLoginDialog: Dispatch<SetStateAction<boolean>>;
    usersRegistered: User[];
    isWsConnected: boolean;
 }
) {

  const selectedUserRef = useRef<HTMLSelectElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const storedUserId = sessionStorage.getItem("userId");
    if (storedUserId && selectedUserRef.current) {
      const optionExists = Array.from(selectedUserRef.current.options).some(
        (opt) => opt.value === storedUserId
      );
      if (optionExists) {
        selectedUserRef.current.value = storedUserId;
      }
    }
  }, []);


  const handleConfirmClick = () => {
     if (!isWsConnected) {
      alert("You are disconnected.");
      setShowLoginDialog(false);    
      return;
    }  
    const selectedUserId : number = Number(selectedUserRef.current!.value);
    const password: string = passwordRef.current?.value.trim() ?? "";

    //console.log("Selected user ID:", selectedUserId);
   
    loginUser(selectedUserId, password); // async call
    setShowLoginDialog(false);    // Close dialog
  };

  const handleCancelClick = () => {
    setShowLoginDialog(false);
  };

  return (
    <div className="dialog-backdrop">
      <div className="dialog">
        <h3>Select User to Login</h3>

        {(
          <select ref={selectedUserRef}>
          {usersRegistered
            .filter(u => !u.isonline )
            .map(u => (
              <option key={u.userId} value={u.userId}>
                {u.fullname}
              </option>
            ))
          }
          </select>
        )}

        {( selectedUserRef &&
          <input 
            placeholder="Password"
            ref={passwordRef}
          ></input>
        )}

        <div className="auth-buttons">
          {(
            <section>
              <button onClick={handleConfirmClick} >
                Confirm
              </button>
              <button onClick={handleCancelClick} >
                Cancel
              </button>
          </section>)}
        </div>
      </div>
    </div>
  );
}

export default LoginDialog;
