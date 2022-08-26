<?php

namespace App\Models;

class ChatRoom extends \CodeIgniter\Model
{
    protected $table = 'ChatRoom';
    protected $allowedFields = [
        'chat_room_no',
        'user_no',
        'user_chat_join_date',
        'chat_room_pk',
        'is_joined'
    ];
    protected $primaryKey = 'chat_room_pk';
}
