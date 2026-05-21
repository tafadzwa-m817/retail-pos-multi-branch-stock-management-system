import {
  Box, Card, CardContent, Typography, TextField, Button, Alert,
  CircularProgress, Divider, Chip, Avatar,
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import LockIcon from '@mui/icons-material/Lock';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Layout } from '../../components/layout/Layout';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import apiClient from '../../api/client';
import type { ApiResponse, UserResponse } from '../../types';

interface PasswordForm {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export const Profile = () => {
  const { user } = useAuth();
  const { showSuccess, showError } = useToast();

  const { data: profile } = useQuery({
    queryKey: ['profile'],
    queryFn: () => apiClient.get<ApiResponse<UserResponse>>('/users/me').then((r) => r.data.data),
  });

  const { register, handleSubmit, watch, reset, formState: { errors } } = useForm<PasswordForm>();

  const passwordMutation = useMutation({
    mutationFn: (data: PasswordForm) =>
      apiClient.put('/users/me/password', {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      }),
    onSuccess: () => { showSuccess('Password changed successfully'); reset(); },
    onError: (e: unknown) => showError((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to change password'),
  });

  const initials = user?.fullName.split(' ').map((n) => n[0]).join('').toUpperCase() ?? 'U';

  return (
    <Layout title="My Profile">
      <Box maxWidth={600} mx="auto">

        {/* Profile Info */}
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Box display="flex" alignItems="center" gap={3} mb={3}>
              <Avatar sx={{ bgcolor: 'primary.main', width: 64, height: 64, fontSize: 24, fontWeight: 700 }}>
                {initials}
              </Avatar>
              <Box>
                <Typography variant="h5" fontWeight={700}>{user?.fullName}</Typography>
                <Typography color="text.secondary">{user?.email}</Typography>
                <Chip label={user?.role?.replace('_', ' ')} color="primary" size="small" sx={{ mt: 0.5 }} />
              </Box>
            </Box>

            <Divider sx={{ mb: 2 }} />

            <Box display="flex" flexDirection="column" gap={1.5}>
              {[
                { label: 'First Name', value: profile?.firstName },
                { label: 'Last Name', value: profile?.lastName },
                { label: 'Email', value: profile?.email },
                { label: 'Role', value: profile?.role?.replace('_', ' ') },
                { label: 'Branch', value: profile?.branchName ?? 'All Branches' },
                { label: 'Account Status', value: profile?.active ? 'Active' : 'Inactive' },
              ].map((row) => (
                <Box key={row.label} display="flex" justifyContent="space-between" alignItems="center">
                  <Typography variant="body2" color="text.secondary" fontWeight={600}>{row.label}</Typography>
                  <Typography variant="body2">{row.value ?? '—'}</Typography>
                </Box>
              ))}
            </Box>
          </CardContent>
        </Card>

        {/* Change Password */}
        <Card>
          <CardContent>
            <Box display="flex" alignItems="center" gap={1} mb={3}>
              <LockIcon color="primary" fontSize="small" />
              <Typography variant="h6" fontWeight={700}>Change Password</Typography>
            </Box>

            <Box component="form" display="flex" flexDirection="column" gap={2}>
              <TextField
                fullWidth size="small" type="password" label="Current Password"
                {...register('currentPassword', { required: 'Required' })}
                error={!!errors.currentPassword} helperText={errors.currentPassword?.message}
              />
              <TextField
                fullWidth size="small" type="password" label="New Password"
                {...register('newPassword', { required: 'Required', minLength: { value: 6, message: 'At least 6 characters' } })}
                error={!!errors.newPassword} helperText={errors.newPassword?.message}
              />
              <TextField
                fullWidth size="small" type="password" label="Confirm New Password"
                {...register('confirmPassword', {
                  required: 'Required',
                  validate: (v) => v === watch('newPassword') || 'Passwords do not match',
                })}
                error={!!errors.confirmPassword} helperText={errors.confirmPassword?.message}
              />
              <Box display="flex" justifyContent="flex-end">
                <Button
                  variant="contained"
                  onClick={handleSubmit((d) => passwordMutation.mutate(d))}
                  disabled={passwordMutation.isPending}
                  startIcon={<LockIcon />}
                >
                  {passwordMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Change Password'}
                </Button>
              </Box>
            </Box>
          </CardContent>
        </Card>
      </Box>
    </Layout>
  );
};
