import { useCallback, useState } from 'react';
import HomePage from './pages/HomePage';
import ScanProgressView from './pages/ScanProgressView';
import ResultsView from './pages/ResultsView';
import GroupDetailView from './pages/GroupDetailView';
import OperationsView from './pages/OperationsView';

type View =
  | { name: 'home' }
  | { name: 'history' }
  | { name: 'progress'; scanId: number }
  | { name: 'results'; scanId: number }
  | { name: 'group'; scanId: number; groupId: number };

export default function App() {
  const [view, setView] = useState<View>({ name: 'home' });

  const goHome = useCallback(() => setView({ name: 'home' }), []);
  const goHistory = useCallback(() => setView({ name: 'history' }), []);
  const onScanCreated = useCallback((scanId: number) => setView({ name: 'progress', scanId }), []);
  const onScanFinished = useCallback(
    (scanId: number) => setView({ name: 'results', scanId }),
    [],
  );
  const openGroup = useCallback(
    (scanId: number, groupId: number) => setView({ name: 'group', scanId, groupId }),
    [],
  );
  const openResults = useCallback((scanId: number) => setView({ name: 'results', scanId }), []);

  return (
    <div className="app">
      <header className="app-header">
        <h1>Image Duplicate Manager</h1>
        <nav className="header-nav">
          <button type="button" className="link-button" onClick={goHome}>
            Inicio
          </button>
          <button type="button" className="link-button" onClick={goHistory}>
            Historial
          </button>
        </nav>
      </header>
      <main className="app-content">
        {view.name === 'home' && (
          <HomePage onScanCreated={onScanCreated} onOpenResults={openResults} />
        )}
        {view.name === 'history' && <OperationsView />}
        {view.name === 'progress' && (
          <ScanProgressView scanId={view.scanId} onFinished={onScanFinished} />
        )}
        {view.name === 'results' && (
          <ResultsView scanId={view.scanId} onOpenGroup={openGroup} />
        )}
        {view.name === 'group' && (
          <GroupDetailView
            scanId={view.scanId}
            groupId={view.groupId}
            onBack={() => openResults(view.scanId)}
          />
        )}
      </main>
    </div>
  );
}


