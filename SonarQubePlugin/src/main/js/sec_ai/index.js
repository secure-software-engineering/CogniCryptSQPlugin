import { Provider } from 'react-redux';
import NavigationBar from './components/NavigationBar';
import store from './store/store';

// This is the entry point of the SecAI Plugin
// can be accessible inside Project -> More

window.registerExtension("secai/sec_ai", () => {
    return(
        <Provider store={store}>
            <NavigationBar />
        </Provider>
    )
});

