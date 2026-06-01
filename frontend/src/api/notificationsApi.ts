// types/notification.ts (או באותו קובץ)
export interface NotificationDTO {
    id: string;
    message: string;
    read: boolean; // שונה מ-isRead ל-read
    createdAt: string; 
    title?: string;
}


export interface CreateNotificationRequestDTO {
  targetUserId: string;
  message: string;
}

export interface BroadcastNotificationRequestDTO {
  message: string;
}

// api/notifications.api.ts
const BASE_URL = '/api/notifications';

// פונקציית עזר לשליפת הטוקן (יש להתאים למנגנון האותנטיקציה שלכם)
const getAuthHeaders = (): HeadersInit => {
  const token = localStorage.getItem('token'); // או כל מקום אחר בו נשמר הטוקן
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };
};

export const NotificationAPI = {
  /**
   * Fetch all notifications for the current user
   */
  getNotifications: async (): Promise<NotificationDTO[]> => {
    const response = await fetch(`${BASE_URL}`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch notifications');
    return response.json();
  },

  /**
   * Get the count of unread notifications
   */
  getUnreadCount: async (): Promise<number> => {
    const response = await fetch(`${BASE_URL}/unread-count`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch unread count');
    return response.json();
  },

  /**
   * Get a specific notification by ID
   */
  getNotificationById: async (id: string): Promise<NotificationDTO> => {
    const response = await fetch(`${BASE_URL}/${id}`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch notification');
    return response.json();
  },

  /**
   * Mark a notification as read
   */
  markAsRead: async (id: string): Promise<boolean> => {
        const response = await fetch(`${BASE_URL}/${id}/read`, {
            method: 'PUT',
            headers: getAuthHeaders(),
        });
        
        if (!response.ok) {
            throw new Error(`Failed to mark as read. Status: ${response.status}`);
        }
        return true;
    },
    markAsUnread: async (id: string): Promise<boolean> => {
        const response = await fetch(`${BASE_URL}/${id}/unread`, {
            method: 'PUT',
            headers: getAuthHeaders(),
        });
        
        if (!response.ok) {
            throw new Error(`Failed to mark as unread. Status: ${response.status}`);
        }
        return true;
    },
  /**
   * Create a new notification (Admin/System)
   */
  createNotification: async (data: CreateNotificationRequestDTO): Promise<NotificationDTO> => {
    const response = await fetch(`${BASE_URL}`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data),
    });
    if (!response.ok) throw new Error('Failed to create notification');
    return response.json();
  }
};