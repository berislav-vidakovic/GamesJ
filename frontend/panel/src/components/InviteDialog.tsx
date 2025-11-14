// LoginDialog.tsx
import type { Dispatch, SetStateAction } from "react";
import type { User } from '@common/interfaces';
import { useRef } from "react";
import { inviteUser } from "../utils.ts";


function InviteDialog({ 
    setShowInviteDialog, usersRegistered, isWsConnected, currentUserId, selectedGame 
  }: { 
    setShowInviteDialog: Dispatch<SetStateAction<boolean>>;
    usersRegistered: User[];
    isWsConnected: boolean;
    currentUserId: number;  
    selectedGame: "panel.game.sudoku" | "panel.game.connect4" | "panel.game.memory" | null;
 }
) {

  const selectedUserRef = useRef<HTMLSelectElement>(null);

  const handleConfirmClick = () => {
     if (!isWsConnected) {
      alert("You are disconnected.");
      setShowInviteDialog(false);    
      return;
    }  
    const selectedUserId : number = Number(selectedUserRef.current!.value);
    //console.log("Selected user ID:", selectedUserId);
   
    inviteUser(currentUserId, selectedUserId, "send", selectedGame); // async call
    setShowInviteDialog(false);    // Close dialog
  };

  const handleCancelClick = () => {
    setShowInviteDialog(false);
  };

  return (
    <div className="dialog-backdrop">
      <div className="dialog">
        <h3>Select User to Invite</h3>

        {(
          <select ref={selectedUserRef}>
          {usersRegistered
            .filter(u => u.isonline && u.userId != currentUserId )
            .map(u => (
              <option key={u.userId} value={u.userId}>
                {u.fullname}
              </option>
            ))
          }
          </select>
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

export default InviteDialog;
