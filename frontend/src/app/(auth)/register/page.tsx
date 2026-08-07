'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { ApiError, googleLoginUrl, register } from '@/lib/auth/api'
import { setAccessToken } from '@/lib/auth/tokens'
import Link from 'next/link'

export default function RegisterPage() {
  const router = useRouter()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      const data = await register({ name, email, password })
      setAccessToken(data.accessToken)
      router.push('/dashboard')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Registration failed')
    }
  }

  const handleGoogleLogin = () => {
    window.location.href = googleLoginUrl()
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#F8FAFC]">
      <div className="bg-white p-8 rounded-lg shadow-sm border border-[#E2E8F0] w-full max-w-md">
        <h2 className="text-2xl font-semibold text-[#0F172A] mb-6 text-center">
          Create an Account
        </h2>

        {error && <div className="mb-4 text-[#DC2626] text-sm text-center">{error}</div>}

        <form onSubmit={handleRegister} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[#0F172A]">Full Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1 w-full px-3 py-2 border border-[#E2E8F0] rounded-md text-[#0F172A] focus:outline-none focus:ring-1 focus:ring-[#2563EB]"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[#0F172A]">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 w-full px-3 py-2 border border-[#E2E8F0] rounded-md text-[#0F172A] focus:outline-none focus:ring-1 focus:ring-[#2563EB]"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[#0F172A]">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 w-full px-3 py-2 border border-[#E2E8F0] rounded-md text-[#0F172A] focus:outline-none focus:ring-1 focus:ring-[#2563EB]"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full bg-[#2563EB] text-white py-2 rounded-md hover:bg-blue-700 transition"
          >
            Sign Up
          </button>
        </form>

        <div className="my-6 flex items-center">
          <div className="flex-grow border-t border-[#E2E8F0]"></div>
          <span className="px-3 text-[#64748B] text-sm">or</span>
          <div className="flex-grow border-t border-[#E2E8F0]"></div>
        </div>

        <button
          onClick={handleGoogleLogin}
          className="w-full border border-[#E2E8F0] text-[#0F172A] py-2 rounded-md hover:bg-[#F1F5F9] transition flex items-center justify-center"
        >
          Continue with Google
        </button>

        <div className="mt-6 text-center text-sm text-[#64748B]">
          Already have an account?{' '}
          <Link href="/login" className="text-[#2563EB] hover:underline">
            Sign in
          </Link>
        </div>
      </div>
    </div>
  )
}
