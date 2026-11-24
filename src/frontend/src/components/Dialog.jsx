import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { useT } from '../prefs.jsx';

const DialogContext = createContext(null);

/**
 * In-app replacement for window.confirm and window.prompt, so questions match the app's look and
 * language. ask({ title, message, input, confirmLabel, danger }) resolves to the typed text
 * (input dialogs), true (confirm) or null when the user cancels.
 */
export function DialogProvider({ children }) {
  const [request, setRequest] = useState(null);

  const ask = useCallback((options) => new Promise((resolve) => setRequest({ ...options, resolve })), []);

  function close(result) {
    request.resolve(result);
    setRequest(null);
  }

  return (
    <DialogContext.Provider value={ask}>
      {children}
      {request && <AskDialog request={request} onClose={close} />}
    </DialogContext.Provider>
  );
}

export function useDialog() {
  return useContext(DialogContext);
}

function AskDialog({ request, onClose }) {
  const t = useT();
  const ref = useRef(null);
  const [value, setValue] = useState(request.input?.value ?? '');

  useEffect(() => {
    ref.current.showModal();
  }, []);

  function submit(event) {
    event.preventDefault();
    onClose(request.input ? value : true);
  }

  return (
    <dialog ref={ref} className="dialog" onCancel={(e) => { e.preventDefault(); onClose(null); }}
      aria-labelledby="dialog-title">
      <form onSubmit={submit}>
        <h2 id="dialog-title">{request.title}</h2>
        {request.message && <p className="muted">{request.message}</p>}
        {request.input && (
          <label className="field">
            <span>{request.input.label}</span>
            <input autoFocus type={request.input.type || 'text'} min={request.input.min} max={request.input.max}
              value={value} onChange={(e) => setValue(e.target.value)} />
          </label>
        )}
        <div className="dialog-actions">
          <button type="button" className="btn" onClick={() => onClose(null)}>{t('Cancel')}</button>
          <button type="submit" className={`btn ${request.danger ? 'btn-danger-solid' : 'btn-primary'}`}
            autoFocus={!request.input}>
            {request.confirmLabel || t('OK')}
          </button>
        </div>
      </form>
    </dialog>
  );
}
