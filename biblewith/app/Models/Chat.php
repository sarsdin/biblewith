<?php

namespace App\Models;

class Chat extends \CodeIgniter\Model
{
    protected $table = 'Chat';
    protected $allowedFields = [
        'chat_no',
        'chat_room_no',
        'user_no',
        'chat_type',
        'chat_content',
        'create_date'
    ];
    protected $primaryKey = 'chat_no';
}
