import React, { useState } from 'react';
import { Box, Fab, Paper, IconButton, TextField, List, ListItem, CircularProgress, Typography, Snackbar, Alert } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import ChatIcon from '@mui/icons-material/Chat';
import CloseIcon from '@mui/icons-material/Close';
import SendIcon from '@mui/icons-material/Send';
import api from '../api/axiosInstance';

const ChatWidget = () => {
  const theme = useTheme();
  const mode = theme.palette.mode;
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [snack, setSnack] = useState({ open: false, message: '', severity: 'info' });

  const toggle = () => setOpen(o => !o);

  const sendMessage = async () => {
    const text = (input || '').trim();
    if (!text) return;
    const userMsg = { from: 'user', text };
    setMessages(m => [...m, userMsg]);
    setInput('');
    setLoading(true);
    try {
      const resp = await api.post('/api/ai/chat', { message: text });
      const aiText = resp?.data?.response || 'No response from assistant.';
      setMessages(m => [...m, { from: 'ai', text: aiText }]);
    } catch (e) {
      console.error('AI chat error', e);
      setSnack({ open: true, message: e?.response?.data?.message || 'AI assistant unavailable', severity: 'error' });
      setMessages(m => [...m, { from: 'ai', text: 'Sorry — the assistant is currently unavailable. Please try again later.' }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Box sx={{ position: 'fixed', right: 20, bottom: 20, zIndex: 1400 }}>
        {open && (
          <Paper elevation={8} sx={{ width: 360, maxWidth: '90vw', height: 480, display: 'flex', flexDirection: 'column', mb: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', p: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
              <Typography variant="subtitle1">FieldFlow Assistant</Typography>
              <IconButton size="small" onClick={toggle}><CloseIcon fontSize="small" /></IconButton>
            </Box>

            <Box sx={{ flex: 1, overflow: 'auto', p: 1, display: 'flex', flexDirection: 'column', gap: 1 }}>
              {messages.map((m, idx) => {
                const isUser = m.from === 'user';
                // bubble colors
                const userBg = theme.palette.primary.main;
                const userColor = theme.palette.primary.contrastText || '#fff';
                const aiBg = mode === 'dark' ? theme.palette.grey[800] : '#fff';
                const aiColor = mode === 'dark' ? '#fff' : '#000';

                return (
                  <Box key={idx} sx={{ display: 'flex', justifyContent: isUser ? 'flex-end' : 'flex-start' }}>
                    <Box
                      sx={{
                        background: isUser ? userBg : aiBg,
                        color: isUser ? userColor : aiColor,
                        px: 2,
                        py: 1.25,
                        borderRadius: 3,
                        maxWidth: '80%',
                        boxShadow: 1,
                        whiteSpace: 'pre-wrap',
                        // speech-bubble like rounded corners
                        borderTopLeftRadius: isUser ? 12 : 2,
                        borderTopRightRadius: isUser ? 2 : 12,
                      }}
                    >
                      <Typography variant="body2" sx={{ fontSize: 13 }}>{m.text}</Typography>
                    </Box>
                  </Box>
                );
              })}

              {loading && (
                <Box sx={{ display: 'flex', justifyContent: 'flex-start', p: 1 }}>
                  <Box sx={{ background: mode === 'dark' ? theme.palette.grey[800] : '#fff', color: mode === 'dark' ? '#fff' : '#000', px: 2, py: 1, borderRadius: 3 }}>
                    <Typography variant="body2">Assistant is typing...</Typography>
                  </Box>
                </Box>
              )}
            </Box>

            <Box sx={{ display: 'flex', gap: 1, p: 1, borderTop: '1px solid', borderColor: 'divider' }}>
              <TextField
                placeholder="Ask the assistant (e.g. 'How do I complete an installation?')"
                size="small"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') sendMessage(); }}
                fullWidth
              />
              <IconButton color="primary" onClick={sendMessage} disabled={loading}>
                <SendIcon />
              </IconButton>
            </Box>
          </Paper>
        )}

        <Fab color="primary" onClick={toggle} aria-label="open chat" sx={{ boxShadow: 4 }}>
          <ChatIcon />
        </Fab>
      </Box>

      <Snackbar open={snack.open} autoHideDuration={6000} onClose={() => setSnack(s => ({ ...s, open: false }))}>
        <Alert severity={snack.severity} onClose={() => setSnack(s => ({ ...s, open: false }))}>{snack.message}</Alert>
      </Snackbar>
    </>
  );
};

export default ChatWidget;
