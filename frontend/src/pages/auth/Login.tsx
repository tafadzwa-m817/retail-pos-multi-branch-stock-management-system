import {
  Box, Card, CardContent, TextField, Button, Typography,
  InputAdornment, IconButton, Alert, CircularProgress,
} from '@mui/material';
import EmailIcon from '@mui/icons-material/Email';
import LockIcon from '@mui/icons-material/Lock';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import StorefrontIcon from '@mui/icons-material/Storefront';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../api/auth';
import { useAuth } from '../../context/AuthContext';
import type { LoginRequest } from '../../types';

export const Login = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<LoginRequest>();

  const onSubmit = async (data: LoginRequest) => {
    setLoading(true);
    setError('');
    try {
      const response = await authApi.login(data);
      login(response);
      navigate('/dashboard');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message ?? 'Invalid email or password';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        bgcolor: '#0D47A1',
        background: 'linear-gradient(135deg, #0D47A1 0%, #1565C0 50%, #1976D2 100%)',
      }}
    >
      {/* Left branding panel */}
      <Box
        sx={{
          flex: 1,
          display: { xs: 'none', md: 'flex' },
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#fff',
          p: 6,
        }}
      >
        <StorefrontIcon sx={{ fontSize: 80, mb: 3, opacity: 0.9 }} />
        <Typography variant="h3" fontWeight={800} mb={1}>
          Retail POS
        </Typography>
        <Typography variant="h6" sx={{ opacity: 0.8, textAlign: 'center', maxWidth: 380 }}>
          Multi-Branch Stock Management & Point of Sale System
        </Typography>
        <Box mt={4} sx={{ opacity: 0.6 }}>
          <Typography variant="body2">Powered by july28 Systems</Typography>
        </Box>
      </Box>

      {/* Right login card */}
      <Box
        sx={{
          width: { xs: '100%', md: 440 },
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          p: 3,
          bgcolor: 'background.default',
          borderRadius: { md: '24px 0 0 24px' },
        }}
      >
        <Card sx={{ width: '100%', maxWidth: 380, boxShadow: 'none', bgcolor: 'transparent' }}>
          <CardContent sx={{ p: 0 }}>
            <Box mb={4}>
              <Typography variant="h5" fontWeight={800} color="text.primary">
                Welcome back
              </Typography>
              <Typography variant="body2" color="text.secondary" mt={0.5}>
                Sign in to your account to continue
              </Typography>
            </Box>

            {error && (
              <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>
                {error}
              </Alert>
            )}

            <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
              <TextField
                fullWidth
                label="Email address"
                type="email"
                autoComplete="email"
                autoFocus
                margin="normal"
                {...register('email', {
                  required: 'Email is required',
                  pattern: { value: /\S+@\S+\.\S+/, message: 'Invalid email' },
                })}
                error={!!errors.email}
                helperText={errors.email?.message}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <EmailIcon fontSize="small" color="action" />
                    </InputAdornment>
                  ),
                }}
              />

              <TextField
                fullWidth
                label="Password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                margin="normal"
                {...register('password', { required: 'Password is required' })}
                error={!!errors.password}
                helperText={errors.password?.message}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <LockIcon fontSize="small" color="action" />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton onClick={() => setShowPassword(!showPassword)} edge="end" size="small">
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />

              <Button
                type="submit"
                fullWidth
                variant="contained"
                size="large"
                disabled={loading}
                sx={{ mt: 3, mb: 2, py: 1.5, fontSize: 16 }}
              >
                {loading ? <CircularProgress size={24} color="inherit" /> : 'Sign In'}
              </Button>
            </Box>

            <Box mt={3} p={2} bgcolor="#F0F4F8" borderRadius={2}>
              <Typography variant="caption" color="text.secondary" fontWeight={600} display="block" mb={1}>
                Demo accounts:
              </Typography>
              {[
                { label: 'Admin', email: 'admin@july28retail.co.zw', pass: 'admin123' },
                { label: 'Manager', email: 'manager@july28retail.co.zw', pass: 'manager123' },
                { label: 'Cashier', email: 'cashier@july28retail.co.zw', pass: 'cashier123' },
              ].map((a) => (
                <Typography key={a.label} variant="caption" color="text.secondary" display="block">
                  <strong>{a.label}:</strong> {a.email} / {a.pass}
                </Typography>
              ))}
            </Box>
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
};
